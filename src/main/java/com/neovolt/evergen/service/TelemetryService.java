package com.neovolt.evergen.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.neovolt.evergen.model.bytewatt.RunningData;
import com.neovolt.evergen.model.bytewatt.SystemInfo;
import com.neovolt.evergen.model.queue.TelemetryData;
import com.neovolt.evergen.model.site.HybridInverter;
import com.neovolt.evergen.model.site.Meter;

import io.cloudevents.CloudEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 遥测服务，用于收集设备数据并发送到平台
 */
@Service
@Slf4j
public class TelemetryService {

    private final CloudEventService cloudEventService;
    private final AmazonSQS sqsClient;
    private final ByteWattService byteWattService;
    
    public TelemetryService(CloudEventService cloudEventService, AmazonSQS sqsClient, ByteWattService byteWattService) {
        this.cloudEventService = cloudEventService;
        this.sqsClient = sqsClient;
        this.byteWattService = byteWattService;
    }
    
    @Value("${source.id:urn:com.neovolt.evergen.device}")
    private String sourceId;
    
    @Value("${sqs.queues.telemetry}")
    private String telemetryQueueUrl;
    
    private static final String TELEMETRY_TYPE = "com.evergen.energy.telemetry.v1";

    /**
     * 定时收集并发送遥测数据
     * 每分钟执行一次，但只在整5分钟时间点（如12:00, 12:05等）发送数据
     */
    @Scheduled(cron = "0 * * * * *") // 每分钟的第0秒执行
    public void scheduledTelemetryCollection() {
        collectAndSendTelemetry();
    }
    
    /**
     * 收集并发送所有设备的遥测数据
     */
    private void collectAndSendTelemetry() {
        try {
            // 获取所有站点ID
            List<String> siteIds = getAllSiteIds();
            
            // 一次性获取所有增强的运行数据（合并了SecondLevelData）
            List<RunningData> enhancedRunningDataList = byteWattService.getEnhancedGroupRunningData();
            
            // 获取系统信息
            List<SystemInfo> systems = byteWattService.getSystemList();
            
            for (String siteId : siteIds) {
                // 收集该站点的所有设备数据
                // 混合逆变器
                List<HybridInverter> hybridInverters = collectHybridInverterData(siteId, enhancedRunningDataList, systems);
                // 电表
                List<Meter> meters = collectMeterData(siteId, enhancedRunningDataList, systems);
                
                // 发送遥测数据
                sendTelemetry(siteId, hybridInverters, meters);
            }
        } catch (Exception e) {
            log.error("Error collecting and sending telemetry data: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取所有站点ID
     * 
     * @return 站点ID列表
     */
    private List<String> getAllSiteIds() {
        // 站点ID就是hybrid inverter sn,这里的逻辑是一个站点只有一个混合逆变器和电表
        List<String> siteIds = new ArrayList<>();
        
        // 从ByteWatt API获取系统列表
        List<SystemInfo> systems = byteWattService.getSystemList();
        
        // 提取系统SN作为站点ID
        for (SystemInfo system : systems) {
            if (system.getSysSn() != null) {
                siteIds.add(system.getSysSn());
            }
        }
        
        if (siteIds.isEmpty()) {
            log.warn("No site obtained!");
        } else {
            log.info("get {} site", siteIds.size());
        }
        
        return siteIds;
    }
    
    /**
     * 收集混合逆变器数据
     * 
     * @param siteId 站点ID
     * @param enhancedRunningDataList 增强的运行数据列表
     * @param systems 系统信息列表
     * @return 混合逆变器数据列表
     */
    private List<HybridInverter> collectHybridInverterData(String siteId, List<RunningData> enhancedRunningDataList, List<SystemInfo> systems) {
        List<HybridInverter> hybridInverters = new ArrayList<>();
        
        SystemInfo systemInfo = null;
        
        // 查找对应站点的系统信息
        for (SystemInfo system : systems) {
            if (siteId.equals(system.getSysSn())) {
                systemInfo = system;
                break;
            }
        }
        
        // 查找对应站点的运行数据
        for (RunningData runningData : enhancedRunningDataList) {
            if (siteId.equals(runningData.getSysSn())) {
                // 转换为HybridInverter对象
                HybridInverter inverter = byteWattService.convertToHybridInverter(runningData, systemInfo);
                
                // 转换上传时间为UTC
                LocalDateTime utcTime = byteWattService.convertToUtcTime(runningData.getUploadDatetime(), systemInfo.getTimezone());
                
                // 设置设备时间为UTC时间
                inverter.setDeviceTime(utcTime);
                
                hybridInverters.add(inverter);
                break;
            }
        }
        
        return hybridInverters;
    }
    
    /**
     * 收集电表数据
     * 
     * @param siteId 站点ID
     * @param enhancedRunningDataList 增强的运行数据列表
     * @param systems 系统信息列表
     * @return 电表数据列表
     */
    private List<Meter> collectMeterData(String siteId, List<RunningData> enhancedRunningDataList, List<SystemInfo> systems) {
        List<Meter> meters = new ArrayList<>();
        
        SystemInfo systemInfo = null;
        
        // 查找对应站点的系统信息
        for (SystemInfo system : systems) {
            if (siteId.equals(system.getSysSn())) {
                systemInfo = system;
                break;
            }
        }
        // 查找对应站点的运行数据
        for (RunningData runningData : enhancedRunningDataList) {
            if (siteId.equals(runningData.getSysSn())) {
                // 转换为Meter对象
                Meter meter = byteWattService.convertToMeter(runningData, systemInfo);
                
                // 转换上传时间为UTC
                LocalDateTime utcTime = byteWattService.convertToUtcTime(runningData.getUploadDatetime(), systemInfo.getTimezone());
                
                // 设置设备时间为UTC时间
                meter.setDeviceTime(utcTime);
                
                meters.add(meter);
                break;
            }
        }
        
        return meters;
    }

    /**
     * 发送遥测数据到平台
     * 
     * @param siteId 站点ID
     * @param batteryInverters 电池逆变器数据列表,当前没有
     * @param hybridInverters 混合逆变器数据列表
     * @param solarInverters 太阳能逆变器数据列表，当前没有
     * @param meters 电表数据列表
     */
    public void sendTelemetry(
            String siteId,
            List<HybridInverter> hybridInverters,
            List<Meter> meters) {
        
        log.info("Sending telemetry data for site: {}", siteId);
        
        // 创建遥测数据对象
        TelemetryData telemetryData = new TelemetryData();
        telemetryData.setSiteId(siteId);
        telemetryData.setHybridInverters(hybridInverters);
        telemetryData.setMeters(meters);
        
        // 创建CloudEvent并发送
        CloudEvent event = cloudEventService.createCloudEvent(TELEMETRY_TYPE, sourceId, telemetryData);
        sendMessage(event);
        
        log.debug("Telemetry data sent successfully for site: {}", siteId);
    }
    
    /**
     * 发送消息到SQS队列
     *
     * @param event CloudEvent事件
     */
    private void sendMessage(CloudEvent event) {
        try {
            String messageBody = cloudEventService.serializeToString(event);
            SendMessageRequest sendMessageRequest = new SendMessageRequest()
                    .withQueueUrl(telemetryQueueUrl)
                    .withMessageBody(messageBody);
            
            sqsClient.sendMessage(sendMessageRequest);
            log.info("Telemetry message sent to queue: {}", telemetryQueueUrl);
        } catch (Exception e) {
            log.error("Failed to send telemetry message to queue {}: {}", telemetryQueueUrl, e.getMessage());
            throw new RuntimeException("Failed to send telemetry message to SQS", e);
        }
    }
}
