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
        
        try {
            // 1. 调用ByteWatt API绑定SN
            boolean bindResult = byteWattService.bindSn(serialNumber);
            if (!bindResult) {
                throw new RuntimeException("failed to bind sn");
            }
            
            // 2. 调用ByteWatt API添加SN到组
            boolean addResult = byteWattService.addSnToGroup(serialNumber);
            if (!addResult) {
                throw new RuntimeException("failed to add group");
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
            
            // 创建响应数据
            OnboardingResponseData responseData = new OnboardingResponseData();
            responseData.setSerialNumber(serialNumber);
            responseData.setDeviceId(serialNumber); // 使用序列号作为设备ID
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
            
            // 发送响应
            CloudEvent responseEvent = cloudEventService.createCloudEvent(
                    ONBOARDING_RESPONSE_TYPE, 
                    sourceId, 
                    responseData);
            
            sendMessage(telemetryQueueUrl, responseEvent);
            
            log.info("succeed to onboarding，sn: {}", serialNumber);
        } catch (Exception e) {
            log.error("fail to onboarding，sn:  {}, error: {}", serialNumber, e.getMessage(), e);
            
            // 发送失败响应
            OnboardingResponseData failureResponse = new OnboardingResponseData();
            failureResponse.setSerialNumber(serialNumber);
            failureResponse.setDeviceId(serialNumber);
            failureResponse.setConnectionStatus(OnboardingResponseData.CONNECTION_STATUS_NOT_CONNECTED);
            failureResponse.setErrorReason(OnboardingResponseData.ERROR_REASON_WRONG_SERIAL);
            
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
        log.info("offboarding sn: {}", serialNumber);
        
        try {
            // 1. 调用ByteWatt API从组中移除SN
            boolean removeResult = byteWattService.removeSnFromGroup(serialNumber);
            if (!removeResult) {
                throw new RuntimeException("fail to remove group");
            }
            
            // 2. 调用ByteWatt API解绑SN
            boolean unbindResult = byteWattService.unbindSn(serialNumber);
            if (!unbindResult) {
                throw new RuntimeException("fail to unbind sn");
            }
            
            // 创建响应数据
            OffboardingResponseData responseData = new OffboardingResponseData();
            responseData.setSerialNumber(serialNumber);
            responseData.setDeviceId(serialNumber); // 使用序列号作为设备ID
            responseData.setConnectionStatus(OffboardingResponseData.CONNECTION_STATUS_CONNECTED);
            
            // 发送响应
            CloudEvent responseEvent = cloudEventService.createCloudEvent(
                    OFFBOARDING_RESPONSE_TYPE, 
                    sourceId, 
                    responseData);
            
            sendMessage(telemetryQueueUrl, responseEvent);
            
            log.info("succeed to offboarding，sn: {}", serialNumber);
        } catch (Exception e) {
            log.error("fail to offboarding, sn: {}, error: {}", serialNumber, e.getMessage(), e);
            
            // 发送失败响应
            OffboardingResponseData failureResponse = new OffboardingResponseData();
            failureResponse.setSerialNumber(serialNumber);
            failureResponse.setDeviceId(serialNumber);
            failureResponse.setConnectionStatus(OffboardingResponseData.CONNECTION_STATUS_NOT_CONNECTED);
            failureResponse.setErrorReason(OffboardingResponseData.ERROR_REASON_WRONG_SERIAL);
            
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
        // 调用ByteWatt API发送自消费模式命令
        // 控制模式1: 自消费模式
        int controlMode = 1;
        String parameter = ""; // 自消费模式不需要额外参数
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
        // 控制模式2: 仅充电自消费模式
        int controlMode = 2;
        String parameter = ""; // 仅充电自消费模式不需要额外参数
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
        // 控制模式3: 充电模式
        int controlMode = 3;
        String parameter = String.valueOf(powerW); // 参数为充电功率
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
        // 控制模式4: 放电模式
        int controlMode = 4;
        String parameter = String.valueOf(powerW); // 参数为放电功率
        int status = 1; // 1表示开始
        
        boolean result = byteWattService.sendDispatchCommand(deviceId, controlMode, durationSeconds, parameter, status);
        
        if (result) {
            log.info("Discharging device: {} at {}W for {} seconds", deviceId, powerW, durationSeconds);
        } else {
            log.error("Failed to start discharging for device: {}", deviceId);
        }
    }
    
    /**
     * 执行功率因数校正命令
     * 
     * @param deviceId 设备ID
     * @param targetPowerFactor 目标功率因数
     * @param durationSeconds 持续时间（秒）
     */
    private void executePowerFactorCorrectionCommand(String deviceId, double targetPowerFactor, int durationSeconds) {
        // 调用ByteWatt API发送功率因数校正命令
        // 控制模式5: 功率因数校正模式
        int controlMode = 5;
        String parameter = String.valueOf(targetPowerFactor); // 参数为目标功率因数
        int status = 1; // 1表示开始
        
        boolean result = byteWattService.sendDispatchCommand(deviceId, controlMode, durationSeconds, parameter, status);
        
        if (result) {
            log.info("Setting power factor correction for device: {} to {} for {} seconds", 
                    deviceId, targetPowerFactor, durationSeconds);
        } else {
            log.error("Failed to set power factor correction for device: {}", deviceId);
        }
    }
    
    /**
     * 执行注入无功功率命令
     * 
     * @param deviceId 设备ID
     * @param reactivePowerVar 无功功率（VAR）
     * @param durationSeconds 持续时间（秒）
     */
    private void executeInjectReactivePowerCommand(String deviceId, int reactivePowerVar, int durationSeconds) {
        // 调用ByteWatt API发送注入无功功率命令
        // 控制模式6: 注入无功功率模式
        int controlMode = 6;
        String parameter = String.valueOf(reactivePowerVar); // 参数为无功功率值
        int status = 1; // 1表示开始
        
        boolean result = byteWattService.sendDispatchCommand(deviceId, controlMode, durationSeconds, parameter, status);
        
        if (result) {
            log.info("Injecting reactive power for device: {} at {}VAR for {} seconds", 
                    deviceId, reactivePowerVar, durationSeconds);
        } else {
            log.error("Failed to inject reactive power for device: {}", deviceId);
        }
    }
    
    /**
     * 执行吸收无功功率命令
     * 
     * @param deviceId 设备ID
     * @param reactivePowerVar 无功功率（VAR）
     * @param durationSeconds 持续时间（秒）
     */
    private void executeAbsorbReactivePowerCommand(String deviceId, int reactivePowerVar, int durationSeconds) {
        // 调用ByteWatt API发送吸收无功功率命令
        // 控制模式7: 吸收无功功率模式
        int controlMode = 7;
        String parameter = String.valueOf(reactivePowerVar); // 参数为无功功率值
        int status = 1; // 1表示开始
        
        boolean result = byteWattService.sendDispatchCommand(deviceId, controlMode, durationSeconds, parameter, status);
        
        if (result) {
            log.info("Absorbing reactive power for device: {} at {}VAR for {} seconds", 
                    deviceId, reactivePowerVar, durationSeconds);
        } else {
            log.error("Failed to absorb reactive power for device: {}", deviceId);
        }
    }
}
