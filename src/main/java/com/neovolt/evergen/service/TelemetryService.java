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
                // 电池逆变器，这里没有
                List<BatteryInverter> batteryInverters = new ArrayList<>();
                // pv逆变器，这里没有
                List<SolarInverter> solarInverters = new ArrayList<>();
                // 混合逆变器，是有的
                List<HybridInverter> hybridInverters = collectHybridInverterData(siteId);
                // 电表，是有的
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
        // 站点ID就是hybrid inverter sn,这里的逻辑是一个站点只有一个混合逆变器和电表
        List<String> siteIds = new ArrayList<>();
        // 这里要从Bytewatt api数据了
        return siteIds;
    }
    
    /**
     * 收集混合逆变器数据
     * 
     * @param siteId 站点ID
     * @return 混合逆变器数据列表
     */
    private List<HybridInverter> collectHybridInverterData(String siteId) {
        List<HybridInverter> hybridInverters = new ArrayList<>();
        // 当前的逻辑是一个站点下只有一个混合逆变器
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
     * 收集电表数据
     * 
     * @param siteId 站点ID
     * @return 电表数据列表
     */
    private List<Meter> collectMeterData(String siteId) {
        List<Meter> meters = new ArrayList<>();
        // 当前的逻辑是一个站点下只有一个电表
        Meter meter = new Meter();
        meter.setDeviceId(siteId);
        meter.setDeviceTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        meter.setPowerW(0);
        meter.setReactivePowerVar(0);
        meter.setGridVoltage1V(0.0);
        meter.setGridVoltage2V(0.0);
        meter.setGridVoltage3V(0.0);
        meter.setGridFrequencyHz(0.0);
        meter.setCumulativeGridImportWh(0.0);
        meter.setCumulativeGridExportWh(0.0);

        meters.add(meter);
        return meters;
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
