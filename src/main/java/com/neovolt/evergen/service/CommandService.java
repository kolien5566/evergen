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
        log.debug("开始轮询命令队列");
        try {
            // 接收消息
            ReceiveMessageRequest receiveRequest = new ReceiveMessageRequest()
                    .withQueueUrl(commandQueueUrl)
                    .withMaxNumberOfMessages(10)
                    .withWaitTimeSeconds(5);
            
            List<Message> messages = sqsClient.receiveMessage(receiveRequest).getMessages();
            
            if (!messages.isEmpty()) {
                log.info("收到 {} 条命令消息", messages.size());
                
                for (Message message : messages) {
                    try {
                        // 处理消息
                        processMessage(message);
                        
                        // 删除消息
                        deleteMessage(commandQueueUrl, message.getReceiptHandle());
                    } catch (Exception e) {
                        log.error("处理命令消息失败: {}", e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("轮询命令队列失败: {}", e.getMessage(), e);
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
            
            log.info("处理消息，类型: {}", eventType);
            
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
                log.warn("未知的消息类型: {}", eventType);
            }
        } catch (Exception e) {
            log.error("解析或处理消息失败: {}", e.getMessage(), e);
            throw new RuntimeException("处理消息失败", e);
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
            log.debug("已删除消息");
        } catch (Exception e) {
            log.error("删除消息失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除消息失败", e);
        }
    }
    
    /**
     * 处理上线请求
     * 
     * @param requestData 上线请求数据
     */
    private void processOnboardingRequest(OnboardingRequestData requestData) {
        String serialNumber = requestData.getSerialNumber();
        log.info("处理设备上线请求，序列号: {}", serialNumber);
        
        try {
            // TODO: 实现设备上线逻辑
            // 1. 验证序列号是否有效
            // 2. 检查设备是否在线
            // 3. 获取设备静态数据
            
            // 创建响应数据
            OnboardingResponseData responseData = new OnboardingResponseData();
            responseData.setSerialNumber(serialNumber);
            responseData.setDeviceId("device-" + serialNumber); // 示例设备ID生成
            responseData.setConnectionStatus(OnboardingResponseData.CONNECTION_STATUS_CONNECTED);
            
            // 设置站点静态数据（实际应从数据库或设备获取）
            SiteStaticData siteStaticData = new SiteStaticData();
            siteStaticData.setSiteId("site-001");
            responseData.setSiteStaticData(siteStaticData);
            
            // 发送响应
            CloudEvent responseEvent = cloudEventService.createCloudEvent(
                    ONBOARDING_RESPONSE_TYPE, 
                    sourceId, 
                    responseData);
            
            sendMessage(telemetryQueueUrl, responseEvent);
            
            log.info("设备上线响应已发送，序列号: {}", serialNumber);
        } catch (Exception e) {
            log.error("处理设备上线请求失败，序列号: {}, 错误: {}", serialNumber, e.getMessage(), e);
            
            // 发送失败响应
            OnboardingResponseData failureResponse = new OnboardingResponseData();
            failureResponse.setSerialNumber(serialNumber);
            failureResponse.setConnectionStatus(OnboardingResponseData.CONNECTION_STATUS_NOT_CONNECTED);
            failureResponse.setErrorReason(OnboardingResponseData.ERROR_REASON_DEVICE_OFFLINE);
            
            CloudEvent responseEvent = cloudEventService.createCloudEvent(
                    ONBOARDING_RESPONSE_TYPE, 
                    sourceId, 
                    failureResponse);
            
            sendMessage(telemetryQueueUrl, responseEvent);
        }
    }
    
    /**
     * 处理下线请求
     * 
     * @param requestData 下线请求数据
     */
    private void processOffboardingRequest(OffboardingRequestData requestData) {
        String serialNumber = requestData.getSerialNumber();
        log.info("处理设备下线请求，序列号: {}", serialNumber);
        
        try {
            // TODO: 实现设备下线逻辑
            // 1. 验证序列号是否有效
            // 2. 从系统中移除设备
            
            // 创建响应数据
            OffboardingResponseData responseData = new OffboardingResponseData();
            responseData.setSerialNumber(serialNumber);
            responseData.setDeviceId("device-" + serialNumber); // 示例设备ID
            responseData.setConnectionStatus(OffboardingResponseData.CONNECTION_STATUS_NOT_CONNECTED);
            
            // 发送响应
            CloudEvent responseEvent = cloudEventService.createCloudEvent(
                    OFFBOARDING_RESPONSE_TYPE, 
                    sourceId, 
                    responseData);
            
            sendMessage(telemetryQueueUrl, responseEvent);
            
            log.info("设备下线响应已发送，序列号: {}", serialNumber);
        } catch (Exception e) {
            log.error("处理设备下线请求失败，序列号: {}, 错误: {}", serialNumber, e.getMessage(), e);
            
            // 发送失败响应
            OffboardingResponseData failureResponse = new OffboardingResponseData();
            failureResponse.setSerialNumber(serialNumber);
            failureResponse.setConnectionStatus(OffboardingResponseData.CONNECTION_STATUS_CONNECTED);
            failureResponse.setErrorReason(OffboardingResponseData.ERROR_REASON_DEVICE_OFFLINE);
            
            CloudEvent responseEvent = cloudEventService.createCloudEvent(
                    OFFBOARDING_RESPONSE_TYPE, 
                    sourceId, 
                    failureResponse);
            
            sendMessage(telemetryQueueUrl, responseEvent);
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
            log.info("消息已发送到队列: {}", queueUrl);
        } catch (Exception e) {
            log.error("发送消息到队列失败 {}: {}", queueUrl, e.getMessage(), e);
            throw new RuntimeException("发送消息到SQS失败", e);
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
            // 检查命令类型并执行相应操作
            if (commandData.getRealMode() != null) {
                // 处理实功率模式命令
                processRealModeCommand(deviceId, commandData);
            }
            
            if (commandData.getReactiveMode() != null) {
                // 处理无功功率模式命令
                processReactiveModeCommand(deviceId, commandData);
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
            // TODO: 调用设备接口执行自消费模式命令
            executeSelfConsumptionCommand(deviceId, commandData.getDurationSeconds());
        } else if (commandData.getRealMode().getChargeOnlySelfConsumptionCommand() != null) {
            // 仅充电自消费模式命令
            log.info("Executing charge-only self consumption command for device: {}", deviceId);
            // TODO: 调用设备接口执行仅充电自消费模式命令
            executeChargeOnlySelfConsumptionCommand(deviceId, commandData.getDurationSeconds());
        } else if (commandData.getRealMode().getChargeCommand() != null) {
            // 充电命令
            int powerW = commandData.getRealMode().getChargeCommand().getPowerW();
            log.info("Executing charge command for device: {}, power: {}W", deviceId, powerW);
            // TODO: 调用设备接口执行充电命令
            executeChargeCommand(deviceId, powerW, commandData.getDurationSeconds());
        } else if (commandData.getRealMode().getDischargeCommand() != null) {
            // 放电命令
            int powerW = commandData.getRealMode().getDischargeCommand().getPowerW();
            log.info("Executing discharge command for device: {}, power: {}W", deviceId, powerW);
            // TODO: 调用设备接口执行放电命令
            executeDischargeCommand(deviceId, powerW, commandData.getDurationSeconds());
        }
    }
    
    /**
     * 处理无功功率模式命令
     *
     * @param deviceId 设备ID
     * @param commandData 命令数据
     */
    private void processReactiveModeCommand(String deviceId, CommandData commandData) {
        if (commandData.getReactiveMode().getPowerFactorCorrection() != null) {
            // 功率因数校正命令
            double targetPowerFactor = commandData.getReactiveMode().getPowerFactorCorrection().getTargetPowerFactor();
            log.info("Executing power factor correction command for device: {}, target: {}", 
                    deviceId, targetPowerFactor);
            // TODO: 调用设备接口执行功率因数校正命令
            executePowerFactorCorrectionCommand(deviceId, targetPowerFactor, commandData.getDurationSeconds());
        } else if (commandData.getReactiveMode().getInject() != null) {
            // 注入无功功率命令
            int reactivePowerVar = commandData.getReactiveMode().getInject().getReactivePowerVar();
            log.info("Executing inject reactive power command for device: {}, power: {}VAR", 
                    deviceId, reactivePowerVar);
            // TODO: 调用设备接口执行注入无功功率命令
            executeInjectReactivePowerCommand(deviceId, reactivePowerVar, commandData.getDurationSeconds());
        } else if (commandData.getReactiveMode().getAbsorb() != null) {
            // 吸收无功功率命令
            int reactivePowerVar = commandData.getReactiveMode().getAbsorb().getReactivePowerVar();
            log.info("Executing absorb reactive power command for device: {}, power: {}VAR", 
                    deviceId, reactivePowerVar);
            // TODO: 调用设备接口执行吸收无功功率命令
            executeAbsorbReactivePowerCommand(deviceId, reactivePowerVar, commandData.getDurationSeconds());
        }
    }
    
    /**
     * 执行自消费模式命令
     * 
     * @param deviceId 设备ID
     * @param durationSeconds 持续时间（秒）
     */
    private void executeSelfConsumptionCommand(String deviceId, int durationSeconds) {
        // TODO: 实现设备自消费模式控制
        log.info("Self consumption mode activated for device: {} for {} seconds", deviceId, durationSeconds);
    }
    
    /**
     * 执行仅充电自消费模式命令
     * 
     * @param deviceId 设备ID
     * @param durationSeconds 持续时间（秒）
     */
    private void executeChargeOnlySelfConsumptionCommand(String deviceId, int durationSeconds) {
        // TODO: 实现设备仅充电自消费模式控制
        log.info("Charge-only self consumption mode activated for device: {} for {} seconds", deviceId, durationSeconds);
    }
    
    /**
     * 执行充电命令
     * 
     * @param deviceId 设备ID
     * @param powerW 充电功率（瓦特）
     * @param durationSeconds 持续时间（秒）
     */
    private void executeChargeCommand(String deviceId, int powerW, int durationSeconds) {
        // TODO: 实现设备充电控制
        log.info("Charging device: {} at {}W for {} seconds", deviceId, powerW, durationSeconds);
    }
    
    /**
     * 执行放电命令
     * 
     * @param deviceId 设备ID
     * @param powerW 放电功率（瓦特）
     * @param durationSeconds 持续时间（秒）
     */
    private void executeDischargeCommand(String deviceId, int powerW, int durationSeconds) {
        // TODO: 实现设备放电控制
        log.info("Discharging device: {} at {}W for {} seconds", deviceId, powerW, durationSeconds);
    }
    
    /**
     * 执行功率因数校正命令
     * 
     * @param deviceId 设备ID
     * @param targetPowerFactor 目标功率因数
     * @param durationSeconds 持续时间（秒）
     */
    private void executePowerFactorCorrectionCommand(String deviceId, double targetPowerFactor, int durationSeconds) {
        // TODO: 实现设备功率因数校正控制
        log.info("Setting power factor correction for device: {} to {} for {} seconds", 
                deviceId, targetPowerFactor, durationSeconds);
    }
    
    /**
     * 执行注入无功功率命令
     * 
     * @param deviceId 设备ID
     * @param reactivePowerVar 无功功率（VAR）
     * @param durationSeconds 持续时间（秒）
     */
    private void executeInjectReactivePowerCommand(String deviceId, int reactivePowerVar, int durationSeconds) {
        // TODO: 实现设备注入无功功率控制
        log.info("Injecting reactive power for device: {} at {}VAR for {} seconds", 
                deviceId, reactivePowerVar, durationSeconds);
    }
    
    /**
     * 执行吸收无功功率命令
     * 
     * @param deviceId 设备ID
     * @param reactivePowerVar 无功功率（VAR）
     * @param durationSeconds 持续时间（秒）
     */
    private void executeAbsorbReactivePowerCommand(String deviceId, int reactivePowerVar, int durationSeconds) {
        // TODO: 实现设备吸收无功功率控制
        log.info("Absorbing reactive power for device: {} at {}VAR for {} seconds", 
                deviceId, reactivePowerVar, durationSeconds);
    }
}
