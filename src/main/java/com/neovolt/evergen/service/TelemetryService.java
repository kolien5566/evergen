package com.neovolt.evergen.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.neovolt.evergen.model.queue.TelemetryData;
import com.neovolt.evergen.model.site.BatteryInverter;
import com.neovolt.evergen.model.site.HybridInverter;
import com.neovolt.evergen.model.site.Meter;
import com.neovolt.evergen.model.site.SolarInverter;

import io.cloudevents.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 遥测服务，用于收集设备数据并发送到平台
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TelemetryService {

    private final CloudEventService cloudEventService;
    private final AmazonSQS sqsClient;
    
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
        LocalDateTime now = LocalDateTime.now();
        
        // 只在整5分钟时间点发送数据
        if (now.getMinute() % 5 == 0) {
            log.info("执行定时遥测数据收集和发送，时间: {}", now);
            collectAndSendTelemetry();
        }
    }
    
    /**
     * 收集并发送所有设备的遥测数据
     */
    private void collectAndSendTelemetry() {
        try {
            // 获取所有站点ID
            List<String> siteIds = getAllSiteIds();
            
            for (String siteId : siteIds) {
                // 收集该站点的所有设备数据
                List<BatteryInverter> batteryInverters = collectBatteryInverterData(siteId);
                List<HybridInverter> hybridInverters = collectHybridInverterData(siteId);
                List<SolarInverter> solarInverters = collectSolarInverterData(siteId);
                List<Meter> meters = collectMeterData(siteId);
                
                // 发送遥测数据
                sendTelemetry(siteId, batteryInverters, hybridInverters, solarInverters, meters);
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
        // TODO: 实现从数据库或配置中获取所有站点ID
        // 这里只是模拟返回一个站点ID
        List<String> siteIds = new ArrayList<>();
        siteIds.add("site-001");
        return siteIds;
    }
    
    /**
     * 收集电池逆变器数据
     * 
     * @param siteId 站点ID
     * @return 电池逆变器数据列表
     */
    private List<BatteryInverter> collectBatteryInverterData(String siteId) {
        // TODO: 实现从设备接口获取电池逆变器数据
        // 这里只是模拟返回一个电池逆变器数据
        return new ArrayList<>();
    }
    
    /**
     * 收集混合逆变器数据
     * 
     * @param siteId 站点ID
     * @return 混合逆变器数据列表
     */
    private List<HybridInverter> collectHybridInverterData(String siteId) {
        // TODO: 实现从设备接口获取混合逆变器数据
        // 这里只是返回空列表，实际中需要实现
        List<HybridInverter> hybridInverters = new ArrayList<>();
        
        HybridInverter hybridInverter = new HybridInverter();
        hybridInverter.setDeviceId("battery-inverter-001");
        hybridInverter.setDeviceTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        hybridInverter.setBatteryPowerW(1000);
        hybridInverter.setMeterPowerW(2000);
        hybridInverter.setSolarPowerW(3000);
        hybridInverter.setBatteryReactivePowerVar(100);
        hybridInverter.setMeterReactivePowerVar(300);
        hybridInverter.setGridVoltage1V(230.5);
        hybridInverter.setGridFrequencyHz(50.0);
        hybridInverter.setCumulativeBatteryChargeEnergyWh(10000.0);
        hybridInverter.setCumulativeBatteryDischargeEnergyWh(8000.0);
        hybridInverter.setCumulativePvGenerationWh(15000.0);
        hybridInverter.setCumulativeGridImportWh(5000.0);
        hybridInverter.setCumulativeGridExportWh(3000.0);
        hybridInverter.setStateOfCharge(0.8);
        hybridInverter.setStateOfHealth(0.95);
        hybridInverter.setMaxChargePowerW(5000);
        hybridInverter.setMaxDischargePowerW(5000);
        
        hybridInverters.add(hybridInverter);
        return hybridInverters;
    }
    
    /**
     * 收集太阳能逆变器数据
     * 
     * @param siteId 站点ID
     * @return 太阳能逆变器数据列表
     */
    private List<SolarInverter> collectSolarInverterData(String siteId) {
        // TODO: 实现从设备接口获取太阳能逆变器数据
        // 这里只是返回空列表，实际中需要实现
        return new ArrayList<>();
    }
    
    /**
     * 收集电表数据
     * 
     * @param siteId 站点ID
     * @return 电表数据列表
     */
    private List<Meter> collectMeterData(String siteId) {
        // TODO: 实现从设备接口获取电表数据
        // 这里只是返回空列表，实际中需要实现
        return new ArrayList<>();
    }

    /**
     * 发送遥测数据到平台
     * 
     * @param siteId 站点ID
     * @param batteryInverters 电池逆变器数据列表
     * @param hybridInverters 混合逆变器数据列表
     * @param solarInverters 太阳能逆变器数据列表
     * @param meters 电表数据列表
     */
    public void sendTelemetry(
            String siteId,
            List<BatteryInverter> batteryInverters,
            List<HybridInverter> hybridInverters,
            List<SolarInverter> solarInverters,
            List<Meter> meters) {
        
        log.info("Sending telemetry data for site: {}", siteId);
        
        // 创建遥测数据对象
        TelemetryData telemetryData = new TelemetryData();
        telemetryData.setSiteId(siteId);
        telemetryData.setBatteryInverters(batteryInverters);
        telemetryData.setHybridInverters(hybridInverters);
        telemetryData.setSolarInverters(solarInverters);
        telemetryData.setMeters(meters);
        
        // 创建CloudEvent并发送
        CloudEvent event = cloudEventService.createCloudEvent(TELEMETRY_TYPE, sourceId, telemetryData);
        sendMessage(event);
        
        log.debug("Telemetry data sent successfully for site: {}", siteId);
    }
    
    /**
     * 发送单个电池逆变器的遥测数据
     * 
     * @param siteId 站点ID
     * @param batteryInverter 电池逆变器数据
     */
    public void sendBatteryInverterTelemetry(String siteId, BatteryInverter batteryInverter) {
        log.info("Sending battery inverter telemetry data for site: {}, device: {}", 
                siteId, batteryInverter.getDeviceId());
        
        TelemetryData telemetryData = new TelemetryData();
        telemetryData.setSiteId(siteId);
        telemetryData.setBatteryInverters(List.of(batteryInverter));
        
        CloudEvent event = cloudEventService.createCloudEvent(TELEMETRY_TYPE, sourceId, telemetryData);
        sendMessage(event);
    }
    
    /**
     * 发送单个混合逆变器的遥测数据
     * 
     * @param siteId 站点ID
     * @param hybridInverter 混合逆变器数据
     */
    public void sendHybridInverterTelemetry(String siteId, HybridInverter hybridInverter) {
        log.info("Sending hybrid inverter telemetry data for site: {}, device: {}", 
                siteId, hybridInverter.getDeviceId());
        
        TelemetryData telemetryData = new TelemetryData();
        telemetryData.setSiteId(siteId);
        telemetryData.setHybridInverters(List.of(hybridInverter));
        
        CloudEvent event = cloudEventService.createCloudEvent(TELEMETRY_TYPE, sourceId, telemetryData);
        sendMessage(event);
    }
    
    /**
     * 发送单个太阳能逆变器的遥测数据
     * 
     * @param siteId 站点ID
     * @param solarInverter 太阳能逆变器数据
     */
    public void sendSolarInverterTelemetry(String siteId, SolarInverter solarInverter) {
        log.info("Sending solar inverter telemetry data for site: {}, device: {}", 
                siteId, solarInverter.getDeviceId());
        
        TelemetryData telemetryData = new TelemetryData();
        telemetryData.setSiteId(siteId);
        telemetryData.setSolarInverters(List.of(solarInverter));
        
        CloudEvent event = cloudEventService.createCloudEvent(TELEMETRY_TYPE, sourceId, telemetryData);
        sendMessage(event);
    }
    
    /**
     * 发送单个电表的遥测数据
     * 
     * @param siteId 站点ID
     * @param meter 电表数据
     */
    public void sendMeterTelemetry(String siteId, Meter meter) {
        log.info("Sending meter telemetry data for site: {}, device: {}", 
                siteId, meter.getDeviceId());
        
        TelemetryData telemetryData = new TelemetryData();
        telemetryData.setSiteId(siteId);
        telemetryData.setMeters(List.of(meter));
        
        CloudEvent event = cloudEventService.createCloudEvent(TELEMETRY_TYPE, sourceId, telemetryData);
        sendMessage(event);
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
