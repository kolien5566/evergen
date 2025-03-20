package com.neovolt.evergen.service;

import com.amazonaws.services.sqs.model.Message;
import com.neovolt.evergen.model.cloudevent.OnboardingRequestData;
import com.neovolt.evergen.model.cloudevent.OnboardingResponseData;
import com.neovolt.evergen.model.cloudevent.OffboardingRequestData;
import com.neovolt.evergen.model.cloudevent.OffboardingResponseData;
import com.neovolt.evergen.model.site.SiteStaticData;
import io.cloudevents.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 设备上下线服务，处理设备的上线和下线流程
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BoardingService {
    
    private final CloudEventService cloudEventService;
    private final SqsService sqsService;
    
    @Value("${sqs.queues.onboarding}")
    private String onboardingQueueUrl;
    
    @Value("${sqs.queues.offboarding}")
    private String offboardingQueueUrl;
    
    @Value("${source.id:urn:com.neovolt.evergen.device}")
    private String sourceId;
    
    private static final String ONBOARDING_REQUEST_TYPE = "com.evergen.energy.onboarding-request.v1";
    private static final String ONBOARDING_RESPONSE_TYPE = "com.evergen.energy.onboarding-response.v1";
    private static final String OFFBOARDING_REQUEST_TYPE = "com.evergen.energy.offboarding-request.v1";
    private static final String OFFBOARDING_RESPONSE_TYPE = "com.evergen.energy.offboarding-response.v1";
    
    /**
     * 处理上线请求
     *
     * @param maxMessages 最大消息数量
     * @param waitTimeSeconds 等待时间（秒）
     */
    public void processOnboardingRequests(int maxMessages, int waitTimeSeconds) {
        List<Message> messages = sqsService.receiveMessages(onboardingQueueUrl, maxMessages, waitTimeSeconds);
        
        for (Message message : messages) {
            try {
                CloudEvent event = cloudEventService.deserializeFromString(message.getBody());
                String eventType = event.getType();
                
                if (ONBOARDING_REQUEST_TYPE.equals(eventType)) {
                    OnboardingRequestData requestData = cloudEventService.extractData(event, OnboardingRequestData.class);
                    processOnboardingRequest(requestData);
                } else {
                    log.warn("Unexpected event type in onboarding queue: {}", eventType);
                }
                
                // 处理完成后删除消息
                sqsService.deleteMessage(onboardingQueueUrl, message.getReceiptHandle());
            } catch (Exception e) {
                log.error("Error processing onboarding request: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 处理下线请求
     *
     * @param maxMessages 最大消息数量
     * @param waitTimeSeconds 等待时间（秒）
     */
    public void processOffboardingRequests(int maxMessages, int waitTimeSeconds) {
        List<Message> messages = sqsService.receiveMessages(offboardingQueueUrl, maxMessages, waitTimeSeconds);
        
        for (Message message : messages) {
            try {
                CloudEvent event = cloudEventService.deserializeFromString(message.getBody());
                String eventType = event.getType();
                
                if (OFFBOARDING_REQUEST_TYPE.equals(eventType)) {
                    OffboardingRequestData requestData = cloudEventService.extractData(event, OffboardingRequestData.class);
                    processOffboardingRequest(requestData);
                } else {
                    log.warn("Unexpected event type in offboarding queue: {}", eventType);
                }
                
                // 处理完成后删除消息
                sqsService.deleteMessage(offboardingQueueUrl, message.getReceiptHandle());
            } catch (Exception e) {
                log.error("Error processing offboarding request: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 处理单个上线请求
     *
     * @param requestData 上线请求数据
     */
    private void processOnboardingRequest(OnboardingRequestData requestData) {
        String serialNumber = requestData.getSerialNumber();
        log.info("Processing onboarding request for device with serial number: {}", serialNumber);
        
        try {
            // 查找设备
            // TODO: 实现设备查找逻辑
            String deviceId = findDeviceIdBySerialNumber(serialNumber);
            
            if (deviceId != null) {
                // 设备存在，准备站点静态数据
                SiteStaticData siteStaticData = prepareSiteStaticData(serialNumber);
                
                // 发送成功响应
                sendOnboardingResponse(serialNumber, deviceId, "connected", null, siteStaticData);
                log.info("Device with serial number {} successfully onboarded with deviceId {}", serialNumber, deviceId);
            } else {
                // 设备不存在，发送失败响应
                sendOnboardingResponse(serialNumber, "", "not-connected", "wrong-serial", null);
                log.warn("Device with serial number {} not found, onboarding failed", serialNumber);
            }
        } catch (Exception e) {
            log.error("Error during onboarding process for device {}: {}", serialNumber, e.getMessage(), e);
            // 发送失败响应
            sendOnboardingResponse(serialNumber, "", "not-connected", "registration-incomplete", null);
        }
    }
    
    /**
     * 处理单个下线请求
     *
     * @param requestData 下线请求数据
     */
    private void processOffboardingRequest(OffboardingRequestData requestData) {
        String serialNumber = requestData.getSerialNumber();
        log.info("Processing offboarding request for device with serial number: {}", serialNumber);
        
        try {
            // 查找设备
            // TODO: 实现设备查找逻辑
            String deviceId = findDeviceIdBySerialNumber(serialNumber);
            
            if (deviceId != null) {
                // 设备存在，执行下线操作
                // TODO: 实现设备下线逻辑
                boolean success = executeDeviceOffboarding(serialNumber, deviceId);
                
                if (success) {
                    // 发送成功响应
                    sendOffboardingResponse(serialNumber, deviceId, "not-connected", null);
                    log.info("Device with serial number {} successfully offboarded", serialNumber);
                } else {
                    // 下线操作失败
                    sendOffboardingResponse(serialNumber, deviceId, "connected", "device-offline");
                    log.warn("Failed to offboard device with serial number {}", serialNumber);
                }
            } else {
                // 设备不存在，发送失败响应
                sendOffboardingResponse(serialNumber, "", "not-connected", "wrong-serial");
                log.warn("Device with serial number {} not found, offboarding ignored", serialNumber);
            }
        } catch (Exception e) {
            log.error("Error during offboarding process for device {}: {}", serialNumber, e.getMessage(), e);
            // 发送通用失败响应
            sendOffboardingResponse(serialNumber, "", "connected", "device-offline");
        }
    }
    
    /**
     * 发送上线响应
     *
     * @param serialNumber 设备序列号
     * @param deviceId 设备ID
     * @param connectionStatus 连接状态
     * @param errorReason 错误原因（如果有）
     * @param siteStaticData 站点静态数据（如果成功）
     */
    private void sendOnboardingResponse(String serialNumber, String deviceId, String connectionStatus, 
                                         String errorReason, SiteStaticData siteStaticData) {
        OnboardingResponseData responseData = new OnboardingResponseData();
        responseData.setSerialNumber(serialNumber);
        responseData.setDeviceId(deviceId);
        responseData.setConnectionStatus(connectionStatus);
        responseData.setErrorReason(errorReason);
        responseData.setSiteStaticData(siteStaticData);
        
        CloudEvent event = cloudEventService.createCloudEvent(ONBOARDING_RESPONSE_TYPE, sourceId, responseData);
        sqsService.sendMessage(onboardingQueueUrl, event);
        
        log.info("Sent onboarding response for device {}: status={}", serialNumber, connectionStatus);
    }
    
    /**
     * 发送下线响应
     *
     * @param serialNumber 设备序列号
     * @param deviceId 设备ID
     * @param connectionStatus 连接状态
     * @param errorReason 错误原因（如果有）
     */
    private void sendOffboardingResponse(String serialNumber, String deviceId, String connectionStatus, String errorReason) {
        OffboardingResponseData responseData = new OffboardingResponseData();
        responseData.setSerialNumber(serialNumber);
        responseData.setDeviceId(deviceId);
        responseData.setConnectionStatus(connectionStatus);
        responseData.setErrorReason(errorReason);
        
        CloudEvent event = cloudEventService.createCloudEvent(OFFBOARDING_RESPONSE_TYPE, sourceId, responseData);
        sqsService.sendMessage(offboardingQueueUrl, event);
        
        log.info("Sent offboarding response for device {}: status={}", serialNumber, connectionStatus);
    }
    
    /**
     * 根据序列号查找设备ID
     *
     * @param serialNumber 设备序列号
     * @return 设备ID，如果未找到返回null
     */
    private String findDeviceIdBySerialNumber(String serialNumber) {
        // TODO: 实现设备查询逻辑
        // 这里只是模拟，实际应该从数据库或其他存储中查询
        log.info("Looking up device ID for serial number: {}", serialNumber);
        return "device-" + serialNumber; // 模拟返回，实际中需替换
    }
    
    /**
     * 准备站点静态数据
     *
     * @param serialNumber 设备序列号
     * @return 站点静态数据
     */
    private SiteStaticData prepareSiteStaticData(String serialNumber) {
        // TODO: 实现站点静态数据准备逻辑
        // 实际中应该从设备管理系统获取完整的站点信息
        log.info("Preparing site static data for device with serial number: {}", serialNumber);
        
        SiteStaticData siteStaticData = new SiteStaticData();
        siteStaticData.setSiteId("site-" + serialNumber);
        // 设置其他必要的站点信息
        
        return siteStaticData;
    }
    
    /**
     * 执行设备下线操作
     *
     * @param serialNumber 设备序列号
     * @param deviceId 设备ID
     * @return 操作是否成功
     */
    private boolean executeDeviceOffboarding(String serialNumber, String deviceId) {
        // TODO: 实现设备下线逻辑
        // 实际中应该更新设备状态，断开连接等
        log.info("Executing offboarding operation for device: {}, serial: {}", deviceId, serialNumber);
        return true; // 模拟成功，实际中需替换
    }
}
