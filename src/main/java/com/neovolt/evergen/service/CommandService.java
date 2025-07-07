package com.neovolt.evergen.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.neovolt.evergen.model.bytewatt.SystemInfo;
import com.neovolt.evergen.model.queue.CommandData;
import com.neovolt.evergen.model.queue.OffboardingRequestData;
import com.neovolt.evergen.model.queue.OffboardingResponseData;
import com.neovolt.evergen.model.queue.OnboardingRequestData;
import com.neovolt.evergen.model.queue.OnboardingResponseData;
import com.neovolt.evergen.model.site.SiteStaticData;

import io.cloudevents.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 命令服务，用于接收并处理平台下发的控制指令
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommandService {

    private final CloudEventService cloudEventService;
    private final AmazonSQS sqsClient;
    private final ByteWattService byteWattService;
    
    @Value("${sqs.queues.command}")
    private String commandQueueUrl;
    
    @Value("${sqs.queues.telemetry}")
    private String telemetryQueueUrl;
    
    @Value("${source.id:urn:com.neovolt.evergen.device}")
    private String sourceId;
    
    private static final String COMMAND_TYPE = "com.evergen.energy.battery-inverter.command.v1";
    private static final String ONBOARDING_REQUEST_TYPE = "com.evergen.energy.onboarding-request.v1";
    private static final String OFFBOARDING_REQUEST_TYPE = "com.evergen.energy.offboarding-request.v1";
    private static final String ONBOARDING_RESPONSE_TYPE = "com.evergen.energy.onboarding-response.v1";
    private static final String OFFBOARDING_RESPONSE_TYPE = "com.evergen.energy.offboarding-response.v1";

    /**
     * 定时轮询命令队列
     * 每10秒执行一次
     */
    @Scheduled(fixedRate = 10000)
    public void pollCommandQueue() {
        try {
            // 接收消息
            ReceiveMessageRequest receiveRequest = new ReceiveMessageRequest()
                    .withQueueUrl(commandQueueUrl)
                    .withMaxNumberOfMessages(10)
                    .withWaitTimeSeconds(5);
            
            List<Message> messages = sqsClient.receiveMessage(receiveRequest).getMessages();
            
            if (!messages.isEmpty()) {
                log.info("get {} commands", messages.size());
                
                for (Message message : messages) {
                    try {
                        // 处理消息
                        processMessage(message);
                        
                        // 删除消息
                        deleteMessage(commandQueueUrl, message.getReceiptHandle());
                    } catch (Exception e) {
                        log.error("failed to process command: {}", e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("failed to poll command queue: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理接收到的消息
     * 
     * @param message SQS消息
     */
    private void processMessage(Message message) {
        try {
            // 解析CloudEvent
            CloudEvent event = cloudEventService.deserializeFromString(message.getBody());
            String eventType = event.getType();
            
            log.info("message type: {}", eventType);
            
            // 根据消息类型分发处理
            if (COMMAND_TYPE.equals(eventType)) {
                // 处理命令消息
                CommandData commandData = cloudEventService.extractData(event, CommandData.class);
                processCommand(commandData);
            } else if (ONBOARDING_REQUEST_TYPE.equals(eventType)) {
                // 处理上线请求
                OnboardingRequestData requestData = cloudEventService.extractData(event, OnboardingRequestData.class);
                processOnboardingRequest(requestData);
            } else if (OFFBOARDING_REQUEST_TYPE.equals(eventType)) {
                // 处理下线请求
                OffboardingRequestData requestData = cloudEventService.extractData(event, OffboardingRequestData.class);
                processOffboardingRequest(requestData);
            } else {
                log.warn("unknown message type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("failed to process message: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 删除SQS队列中的消息
     *
     * @param queueUrl 队列URL
     * @param receiptHandle 消息接收句柄
     */
    private void deleteMessage(String queueUrl, String receiptHandle) {
        try {
            DeleteMessageRequest deleteRequest = new DeleteMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withReceiptHandle(receiptHandle);
            
            sqsClient.deleteMessage(deleteRequest);
            log.debug("message deleted");
        } catch (Exception e) {
            log.error("failed to delete message: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 处理上线请求
     * 
     * @param requestData 上线请求数据
     */
    private void processOnboardingRequest(OnboardingRequestData requestData) {
        String serialNumber = requestData.getSerialNumber();
        log.info("onboarding sn: {}", serialNumber);
        
        // 预先创建响应数据对象
        OnboardingResponseData responseData = new OnboardingResponseData();
        responseData.setSerialNumber(serialNumber);
        responseData.setDeviceId(serialNumber); // 使用序列号作为设备ID
        
        try {
            // 1. 调用ByteWatt API绑定SN
            boolean bindResult = byteWattService.bindSn(serialNumber);
            if (!bindResult) {
                // 获取错误信息
                int errorCode = byteWattService.getLastErrorCode();
                String errorInfo = byteWattService.getLastErrorInfo();
        
                // 如果错误代码是6024，继续执行成功流程
                if (errorCode == 6024) {
                    log.info("错误代码6024, 继续执行成功流程: {}", serialNumber);
                } else {
                    log.warn("绑定SN失败: 序列号={}, 错误代码={}, 原因={}", serialNumber, errorCode, errorInfo);
                    // 其他错误代码，设置错误响应
                    responseData.setConnectionStatus(OnboardingResponseData.CONNECTION_STATUS_NOT_CONNECTED);
                    
                    // 根据错误代码设置错误原因
                    if (errorCode == 6015) {
                        responseData.setErrorReason(OnboardingResponseData.ERROR_REASON_WRONG_SERIAL);
                        log.warn("设备序列号错误 (code 6015): {}", serialNumber);
                    } else if (errorCode == 6026) {
                        responseData.setErrorReason(OnboardingResponseData.ERROR_REASON_IN_OTHER_VPP);
                        log.warn("设备已在其他VPP中 (code 6026): {}", serialNumber);
                    } else {
                        responseData.setErrorReason(OnboardingResponseData.ERROR_REASON_REGISTRATION_INCOMPLETE);
                        log.warn("设备注册未完成: {}, 错误代码: {}", serialNumber, errorCode);
                    }
                    
                    // 发送错误响应
                    sendOnboardingResponse(responseData);
                    return;
                }
            }
            
            // 2. 调用ByteWatt API添加SN到组
            boolean addResult = byteWattService.addSnToGroup(serialNumber);
            if (!addResult) {
                // 获取错误信息
                int errorCode = byteWattService.getLastErrorCode();
                String errorInfo = byteWattService.getLastErrorInfo();
                
                if (errorCode == 6025) {
                    log.info("错误代码6025, 继续执行成功流程: {}", serialNumber);
                } else {
                    log.warn("添加SN到组失败: 序列号={}, 错误代码={}, 原因={}", serialNumber, errorCode, errorInfo);
                    
                    // 设置错误响应
                    responseData.setConnectionStatus(OnboardingResponseData.CONNECTION_STATUS_NOT_CONNECTED);
                    
                    // 根据错误代码设置错误原因
                    if (errorCode == 6026) {
                        responseData.setErrorReason(OnboardingResponseData.ERROR_REASON_IN_OTHER_VPP);
                        log.warn("设备已在其他VPP中 (code 6024): {}", serialNumber);
                    } else {
                        responseData.setErrorReason(OnboardingResponseData.ERROR_REASON_REGISTRATION_INCOMPLETE);
                        log.warn("添加设备到组失败: {}, 错误代码: {}", serialNumber, errorCode);
                    }
                    
                    // 发送错误响应
                    sendOnboardingResponse(responseData);
                    return;
                }
        }
            
            // 获取系统信息用于构建响应
            List<SystemInfo> systems = byteWattService.getSystemList();
            SystemInfo systemInfo = null;
            
            for (SystemInfo system : systems) {
                if (serialNumber.equals(system.getSysSn())) {
                    systemInfo = system;
                    break;
                }
            }
            
            // 设置成功响应
            responseData.setConnectionStatus(OnboardingResponseData.CONNECTION_STATUS_CONNECTED);
            
            // 设置站点静态数据
            if (systemInfo != null) {
                SiteStaticData siteStaticData = byteWattService.convertToSiteStaticData(systemInfo);
                responseData.setSiteStaticData(siteStaticData);
            } else {
                // 如果没有找到系统信息，创建一个基本的站点数据
                SiteStaticData siteStaticData = new SiteStaticData();
                siteStaticData.setSiteId(serialNumber);
                responseData.setSiteStaticData(siteStaticData);
            }
            
            log.info("onboarding成功: {}", serialNumber);
            
            // 发送成功响应
            sendOnboardingResponse(responseData);
        } catch (Exception e) {
            log.error("onboarding过程发生异常: {}, 错误: {}", serialNumber, e.getMessage(), e);
            
            // 设置错误响应
            responseData.setConnectionStatus(OnboardingResponseData.CONNECTION_STATUS_NOT_CONNECTED);
            responseData.setErrorReason(OnboardingResponseData.ERROR_REASON_REGISTRATION_INCOMPLETE);
            
            // 发送错误响应
            sendOnboardingResponse(responseData);
        }
    }
    
    /**
     * 发送上线响应到telemetry队列
     * 
     * @param responseData 响应数据
     */
    private void sendOnboardingResponse(OnboardingResponseData responseData) {
        try {
            CloudEvent responseEvent = cloudEventService.createCloudEvent(
                    ONBOARDING_RESPONSE_TYPE, 
                    sourceId, 
                    responseData);
            
            sendMessage(telemetryQueueUrl, responseEvent);
            
            if (OnboardingResponseData.CONNECTION_STATUS_CONNECTED.equals(responseData.getConnectionStatus())) {
                log.info("已发送成功上线响应: {}", responseData.getSerialNumber());
            } else {
                log.info("已发送失败上线响应: {}, 错误原因: {}", 
                        responseData.getSerialNumber(), responseData.getErrorReason());
            }
        } catch (Exception e) {
            log.error("发送上线响应失败: {}, 错误: {}", responseData.getSerialNumber(), e.getMessage(), e);
        }
    }
    
    /**
     * 处理下线请求
     * 
     * @param requestData 下线请求数据
     */
    private void processOffboardingRequest(OffboardingRequestData requestData) {
        String serialNumber = requestData.getSerialNumber();
        log.info("offboarding sn: {}", serialNumber);
        
        // 预先创建响应数据对象
        OffboardingResponseData responseData = new OffboardingResponseData();
        responseData.setSerialNumber(serialNumber);
        responseData.setDeviceId(serialNumber); // 使用序列号作为设备ID
        
        try {
            // 1. 调用ByteWatt API从组中移除SN
            boolean removeResult = byteWattService.removeSnFromGroup(serialNumber);
            if (!removeResult) {
                // 获取错误信息
                int errorCode = byteWattService.getLastErrorCode();
                String errorInfo = byteWattService.getLastErrorInfo();
                log.warn("从组中移除SN失败: 序列号={}, 错误代码={}, 原因={}", serialNumber, errorCode, errorInfo);
                
                // 设置错误响应
                responseData.setConnectionStatus(OffboardingResponseData.CONNECTION_STATUS_NOT_CONNECTED);
                responseData.setErrorReason(OffboardingResponseData.ERROR_REASON_WRONG_SERIAL);
                
                // 发送错误响应
                sendOffboardingResponse(responseData);
                return;
            }
            
            // 2. 调用ByteWatt API解绑SN
            boolean unbindResult = byteWattService.unbindSn(serialNumber);
            if (!unbindResult) {
                // 获取错误信息
                int errorCode = byteWattService.getLastErrorCode();
                String errorInfo = byteWattService.getLastErrorInfo();
                log.warn("解绑SN失败: 序列号={}, 错误代码={}, 原因={}", serialNumber, errorCode, errorInfo);
                
                // 设置错误响应
                responseData.setConnectionStatus(OffboardingResponseData.CONNECTION_STATUS_NOT_CONNECTED);
                responseData.setErrorReason(OffboardingResponseData.ERROR_REASON_WRONG_SERIAL);
                
                // 发送错误响应
                sendOffboardingResponse(responseData);
                return;
            }
            
            // 设置成功响应
            responseData.setConnectionStatus(OffboardingResponseData.CONNECTION_STATUS_NOT_CONNECTED);
            
            log.info("offboarding成功: {}", serialNumber);
            
            // 发送成功响应
            sendOffboardingResponse(responseData);
        } catch (Exception e) {
            log.error("offboarding过程发生异常: {}, 错误: {}", serialNumber, e.getMessage(), e);
            
            // 设置错误响应
            responseData.setConnectionStatus(OffboardingResponseData.CONNECTION_STATUS_NOT_CONNECTED);
            responseData.setErrorReason(OffboardingResponseData.ERROR_REASON_DEVICE_OFFLINE);
            
            // 发送错误响应
            sendOffboardingResponse(responseData);
        }
    }
    
    /**
     * 发送下线响应到telemetry队列
     * 
     * @param responseData 响应数据
     */
    private void sendOffboardingResponse(OffboardingResponseData responseData) {
        try {
            CloudEvent responseEvent = cloudEventService.createCloudEvent(
                    OFFBOARDING_RESPONSE_TYPE, 
                    sourceId, 
                    responseData);
            
            sendMessage(telemetryQueueUrl, responseEvent);
            
            if (OffboardingResponseData.CONNECTION_STATUS_CONNECTED.equals(responseData.getConnectionStatus())) {
                log.info("已发送成功下线响应: {}", responseData.getSerialNumber());
            } else {
                log.info("已发送失败下线响应: {}, 错误原因: {}", 
                        responseData.getSerialNumber(), responseData.getErrorReason());
            }
        } catch (Exception e) {
            log.error("发送下线响应失败: {}, 错误: {}", responseData.getSerialNumber(), e.getMessage(), e);
        }
    }
    
    /**
     * 发送消息到SQS队列
     *
     * @param queueUrl 队列URL
     * @param event CloudEvent事件
     */
    private void sendMessage(String queueUrl, CloudEvent event) {
        try {
            String messageBody = cloudEventService.serializeToString(event);
            SendMessageRequest sendMessageRequest = new SendMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withMessageBody(messageBody);
            
            sqsClient.sendMessage(sendMessageRequest);
            log.info("message sent to queue: {}", queueUrl);
        } catch (Exception e) {
            log.error("fail to send message to {}: {}", queueUrl, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 处理命令数据
     *
     * @param commandData 命令数据
     */
    public void processCommand(CommandData commandData) {
        String deviceId = commandData.getDeviceId();
        log.info("Processing command for device: {}", deviceId);
        
        try {
            // 只处理实功率模式命令
            if (commandData.getRealMode() != null) {
                processRealModeCommand(deviceId, commandData);
            }
            
            // 无功功率模式命令直接忽略，只记录日志
            if (commandData.getReactiveMode() != null) {
                log.info("Reactive mode command not supported, device: {}", deviceId);
            }
            
            log.info("Command processed successfully for device: {}", deviceId);
        } catch (Exception e) {
            log.error("Error executing command for device {}: {}", deviceId, e.getMessage(), e);
        }
    }
    
    /**
     * 处理实功率模式命令
     *
     * @param deviceId 设备ID
     * @param commandData 命令数据
     */
    private void processRealModeCommand(String deviceId, CommandData commandData) {
        if (commandData.getRealMode().getSelfConsumptionCommand() != null) {
            // 自消费模式命令
            log.info("Executing self consumption command for device: {}", deviceId);
            executeSelfConsumptionCommand(deviceId, commandData.getDurationSeconds());
        } else if (commandData.getRealMode().getChargeOnlySelfConsumptionCommand() != null) {
            // 仅充电自消费模式命令
            log.info("Executing charge-only self consumption command for device: {}", deviceId);
            executeChargeOnlySelfConsumptionCommand(deviceId, commandData.getDurationSeconds());
        } else if (commandData.getRealMode().getChargeCommand() != null) {
            // 充电命令
            int powerW = commandData.getRealMode().getChargeCommand().getPowerW();
            log.info("Executing charge command for device: {}, power: {}W", deviceId, powerW);
            executeChargeCommand(deviceId, powerW, commandData.getDurationSeconds());
        } else if (commandData.getRealMode().getDischargeCommand() != null) {
            // 放电命令
            int powerW = commandData.getRealMode().getDischargeCommand().getPowerW();
            log.info("Executing discharge command for device: {}, power: {}W", deviceId, powerW);
            executeDischargeCommand(deviceId, powerW, commandData.getDurationSeconds());
        }
    }
    
    /**
     * 执行自消费模式命令
     * 
     * @param deviceId 设备ID
     * @param durationSeconds 持续时间（秒）
     */
    private void executeSelfConsumptionCommand(String deviceId, int durationSeconds) {
        // 调用ByteWatt API发送自消费模式命令
        // 控制模式: 自发自用
        int controlMode = 3;
        // 最大功率参数：
        int chargePower = 37000;
        // SOC设为100%，发送值250（250 * 0.4% = 100%）
        String parameter = String.format("%d|0|0|0|0|0|1", chargePower);
        int status = 1; // 1表示开始
        boolean result = byteWattService.sendDispatchCommand(deviceId, controlMode, durationSeconds, parameter, status);
        
        if (result) {
            log.info("Self consumption mode activated for device: {} for {} seconds", deviceId, durationSeconds);
        } else {
            log.error("Failed to activate self consumption mode for device: {}", deviceId);
        }
    }
    
    /**
     * 执行仅充电自消费模式命令
     * 
     * @param deviceId 设备ID
     * @param durationSeconds 持续时间（秒）
     */
    private void executeChargeOnlySelfConsumptionCommand(String deviceId, int durationSeconds) {
        // 调用ByteWatt API发送仅充电自消费模式命令
        // 控制模式: 禁止放电的自发自用
        int controlMode = 1;
        // 最大功率参数：
        int chargePower = 37000;
        // SOC设为100%，发送值250（250 * 0.4% = 100%）
        String parameter = String.format("%d|0|0|0|0|0|1", chargePower);
        int status = 1; // 1表示开始
        boolean result = byteWattService.sendDispatchCommand(deviceId, controlMode, durationSeconds, parameter, status);
        
        if (result) {
            log.info("Charge-only self consumption mode activated for device: {} for {} seconds", deviceId, durationSeconds);
        } else {
            log.error("Failed to activate charge-only self consumption mode for device: {}", deviceId);
        }
    }
    
    /**
     * 执行充电命令
     * 
     * @param deviceId 设备ID
     * @param powerW 充电功率（瓦特）
     * @param durationSeconds 持续时间（秒）
     */
    private void executeChargeCommand(String deviceId, int powerW, int durationSeconds) {
        // 调用ByteWatt API发送充电命令
        // 控制模式: 充电模式
        int controlMode = 2;
        // 计算充电功率参数：基准值32000 - 充电功率
        int chargePower = 32000 - powerW;
        // SOC设为100%，发送值250（250 * 0.4% = 100%）
        String parameter = String.format("%d|0|250|0|0|0|1", chargePower);
        int status = 1; // 1表示开始
        boolean result = byteWattService.sendDispatchCommand(deviceId, controlMode, durationSeconds, parameter, status);
        
        if (result) {
            log.info("Charging device: {} at {}W for {} seconds", deviceId, powerW, durationSeconds);
        } else {
            log.error("Failed to start charging for device: {}", deviceId);
        }
    }
    
    /**
     * 执行放电命令
     * 
     * @param deviceId 设备ID
     * @param powerW 放电功率（瓦特）
     * @param durationSeconds 持续时间（秒）
     */
    private void executeDischargeCommand(String deviceId, int powerW, int durationSeconds) {
        // 调用ByteWatt API发送放电命令
        // 控制模式: 放电模式
        int controlMode = 2;
        // 计算放电功率参数：基准值32000 + 放电功率
        int dischargePower = 32000 + powerW;
        // SOC设为10%，发送值25（25 * 0.4% = 10%）
        String parameter = String.format("%d|0|25|0|0|0|1", dischargePower);
        int status = 1; // 1表示开始
        boolean result = byteWattService.sendDispatchCommand(deviceId, controlMode, durationSeconds, parameter, status);
        
        if (result) {
            log.info("Discharging device: {} at {}W for {} seconds", deviceId, powerW, durationSeconds);
        } else {
            log.error("Failed to start discharging for device: {}", deviceId);
        }
    }
}
