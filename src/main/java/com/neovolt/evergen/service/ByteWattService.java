package com.neovolt.evergen.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.neovolt.evergen.model.bytewatt.ByteWattResponse;
import com.neovolt.evergen.model.bytewatt.RunningData;
import com.neovolt.evergen.model.bytewatt.SystemInfo;
import com.neovolt.evergen.model.bytewatt.SystemListResponse;
import com.neovolt.evergen.model.site.HybridInverter;
import com.neovolt.evergen.model.site.Meter;
import com.neovolt.evergen.model.site.SiteStaticData;

import lombok.extern.slf4j.Slf4j;

/**
 * ByteWatt服务类，提供API调用和数据转换功能
 */
@Service
@Slf4j
public class ByteWattService {

    private final RestTemplate restTemplate;

    @Value("${bytewatt.api.base-url}")
    private String baseUrl;

    @Value("${bytewatt.api.api-key}")
    private String apiKey;

    @Value("${bytewatt.api.key}")
    private String key;

    @Value("${bytewatt.api.group-key}")
    private String groupKey;

    public ByteWattService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 创建认证参数
     *
     * @return 包含认证信息的Map
     */
    private Map<String, Object> createAuthParams() {
        long timestamp = System.currentTimeMillis() / 1000;
        String message = key + timestamp;
        String sign = DigestUtils.sha512Hex(message);

        Map<String, Object> params = new HashMap<>();
        params.put("api_key", apiKey);
        params.put("timestamp", timestamp);
        params.put("sign", sign);

        return params;
    }

    /**
     * 获取组内所有设备的最新运行数据
     *
     * @return 运行数据列表
     */
    public List<RunningData> getGroupRunningData() {
        try {
            Map<String, Object> requestBody = createAuthParams();
            requestBody.put("group_key", groupKey);

            String url = baseUrl + "/Open/Group/GetLastRunningDataByGroup";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<ByteWattResponse<List<RunningData>>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<ByteWattResponse<List<RunningData>>>() {}
            );
            
            return response.getBody() != null ? response.getBody().getData() : new ArrayList<>();
        } catch (Exception e) {
            log.error("Error getting group running data: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 获取系统列表
     *
     * @return 系统信息列表
     */
    public List<SystemInfo> getSystemList() {
        try {
            Map<String, Object> requestBody = createAuthParams();
            requestBody.put("page_index", 1);
            requestBody.put("page_size", 100);  // 获取足够多的系统信息
            
            String url = baseUrl + "/Open/ESS/GetSystemList";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<ByteWattResponse<SystemListResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<ByteWattResponse<SystemListResponse>>() {}
            );
            
            return response.getBody() != null && response.getBody().getData() != null ? 
                   response.getBody().getData().getSystems() : new ArrayList<>();
        } catch (Exception e) {
            log.error("Error getting system list: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 生成SN密码/校验码
     * 
     * @param sn 系统序列号
     * @return 生成的校验码
     */
    public String generateSnPassword(String sn) {
        if (sn == null || sn.isEmpty() || sn.length() < 10) {
            return "";
        }
        
        try {
            // 取值
            String sysCode = sn.substring(0, 2);
            String supCode = sn.substring(2, 5);
            String version = sn.substring(5, 7);
            String year = sn.substring(7, 9);
            String month = sn.length() == 10 ? sn.substring(9, 10) : sn.substring(9, 11);
            String num = sn.substring(sn.length() - 4);
            
            // 分布计算一
            int a = sysCode.charAt(0) * 10;
            int b = sysCode.charAt(1);
            int c = supCode.charAt(0) * 100;
            int d = supCode.charAt(1);
            int e = version.charAt(0) * 10;
            int f = version.charAt(1);
            int g = supCode.charAt(2);
            String h = num.substring(num.length() - 3);
            int i = year.charAt(0) * 10;
            int j = year.charAt(year.length() - 1);
            long a1 = (a + b + c + d + e + f + g + Integer.parseInt(h)) * 527L;
            int a2 = i + j * 73;
            String a3 = Long.toString(a1) + Integer.toString(a2);
            String fist = String.format("%08X", Long.parseLong(a3));
            
            // 分布计算二
            int k = year.charAt(0) * 10;
            int l = year.charAt(year.length() - 1);
            int m = month.charAt(0) * 10;
            int n = month.charAt(month.length() - 1);
            int o = num.charAt(0);
            int p = Integer.parseInt(num.substring(num.length() - 3));
            int q = version.charAt(0) * 10;
            int r = version.charAt(version.length() - 1);
            String two = String.format("%08X", ((((k + l + m + n) * 527 + (o + p) * 19581) + q + r + 2016) * 17));
            
            String s = two.substring(two.length() - 2);
            String t = fist.substring(5, 7);
            String u = two.substring(two.length() - 4);
            String v = u.substring(0, 2);
            return s + t + v;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 绑定SN（onBoarding）
     *
     * @param serialNumber 系统序列号
     * @return 是否绑定成功
     */
    public boolean bindSn(String serialNumber) {
        try {
            String checkCode = generateSnPassword(serialNumber);
            
            Map<String, Object> requestBody = createAuthParams();
            requestBody.put("sys_sn", serialNumber);
            requestBody.put("check_code", checkCode);

            String url = baseUrl + "/Open/ESS/BindSn";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<ByteWattResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                ByteWattResponse.class
            );
            
            return response.getBody() != null && response.getBody().getCode() == 200;
        } catch (Exception e) {
            log.error("Error binding SN: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 添加SN到组
     *
     * @param serialNumber 系统序列号
     * @return 是否添加成功
     */
    public boolean addSnToGroup(String serialNumber) {
        try {
            Map<String, Object> requestBody = createAuthParams();
            requestBody.put("group_key", groupKey);
            requestBody.put("sys_sn", serialNumber);

            String url = baseUrl + "/Open/Group/AddSnByGroup";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<ByteWattResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                ByteWattResponse.class
            );
            
            return response.getBody() != null && response.getBody().getCode() == 200;
        } catch (Exception e) {
            log.error("Error adding SN to group: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解绑SN（设备下线）
     *
     * @param serialNumber 系统序列号
     * @return 是否解绑成功
     */
    public boolean unbindSn(String serialNumber) {
        try {
            Map<String, Object> requestBody = createAuthParams();
            requestBody.put("sys_sn", serialNumber);

            String url = baseUrl + "/Open/ESS/UnBindSn";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<ByteWattResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                ByteWattResponse.class
            );
            
            return response.getBody() != null && response.getBody().getCode() == 200;
        } catch (Exception e) {
            log.error("Error unbinding SN: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从组中移除SN
     *
     * @param serialNumber 系统序列号
     * @return 是否移除成功
     */
    public boolean removeSnFromGroup(String serialNumber) {
        try {
            Map<String, Object> requestBody = createAuthParams();
            requestBody.put("group_key", groupKey);
            requestBody.put("sys_sn", serialNumber);

            String url = baseUrl + "/Open/Group/RemoveSnByGroup";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<ByteWattResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                ByteWattResponse.class
            );
            
            return response.getBody() != null && response.getBody().getCode() == 200;
        } catch (Exception e) {
            log.error("Error removing SN from group: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 发送调度命令
     *
     * @param serialNumber 系统序列号
     * @param controlMode 控制模式
     * @param expireTime 过期时间（秒）
     * @param parameter 参数字符串
     * @param status 状态（1:开始，0:停止）
     * @return 是否发送成功
     */
    public boolean sendDispatchCommand(String serialNumber, int controlMode, 
                                      int expireTime, String parameter, int status) {
        try {
            Map<String, Object> requestBody = createAuthParams();
            requestBody.put("sys_sn", serialNumber);
            requestBody.put("control_mode", controlMode);
            requestBody.put("expire_time", expireTime);
            requestBody.put("parameter", parameter);
            requestBody.put("status", status);

            String url = baseUrl + "/Open/Dispatch/RemoteDispatchBySN";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<ByteWattResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                ByteWattResponse.class
            );
            
            return response.getBody() != null && response.getBody().getCode() == 200;
        } catch (Exception e) {
            log.error("Error sending dispatch command: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 将ByteWatt运行数据转换为HybridInverter对象
     *
     * @param runningData ByteWatt运行数据
     * @param systemInfo 系统信息（可选）
     * @return HybridInverter对象
     */
    public HybridInverter convertToHybridInverter(RunningData runningData, SystemInfo systemInfo) {
        // 计算总电表功率和太阳能功率
        Integer meterPower = calculateTotalMeterPower(runningData);
        Integer solarPower = calculateTotalSolarPower(runningData);
        Integer batteryPower = runningData.getPBat() != null ? runningData.getPBat().intValue() : null;
        
        // 使用setter方法设置属性
        HybridInverter inverter = new HybridInverter();
        try {
            // 设备信息
            inverter.setDeviceId(runningData.getSysSn());
            inverter.setDeviceTime(runningData.getUploadDatetime());
            
            // 功率数据
            inverter.setBatteryPowerW(batteryPower);
            inverter.setMeterPowerW(meterPower);
            inverter.setSolarPowerW(solarPower);
            
            // 无功功率
            inverter.setBatteryReactivePowerVar(0);
            inverter.setMeterReactivePowerVar(0);
            
            // 电网电压和频率
            inverter.setGridVoltage1V(runningData.getUA());
            inverter.setGridVoltage2V(runningData.getUB());
            inverter.setGridVoltage3V(runningData.getUC());
            inverter.setGridFrequencyHz(runningData.getFac());
            
            // 累计能量数据
            inverter.setCumulativeBatteryChargeEnergyWh(runningData.getECharge());
            inverter.setCumulativeBatteryDischargeEnergyWh(runningData.getEInput());
            inverter.setCumulativePvGenerationWh(runningData.getEpvTotal());
            inverter.setCumulativeGridImportWh(runningData.getEInput());
            inverter.setCumulativeGridExportWh(runningData.getEOutput());
            
            // 电池状态
            inverter.setStateOfCharge(runningData.getSoc());
            inverter.setStateOfHealth(100.0);
            
            // 最大充放电功率
            if (systemInfo != null) {
                inverter.setMaxChargePowerW(systemInfo.getPoinv() != null ? systemInfo.getPoinv().intValue() : null);
                inverter.setMaxDischargePowerW(systemInfo.getPoinv() != null ? systemInfo.getPoinv().intValue() : null);
            }
        } catch (Exception e) {
            log.error("Error setting HybridInverter properties: {}", e.getMessage());
        }
        
        return inverter;
    }

    /**
     * 将ByteWatt运行数据转换为Meter对象
     *
     * @param runningData ByteWatt运行数据
     * @return Meter对象
     */
    public Meter convertToMeter(RunningData runningData, SystemInfo systemInfo) {
        // 计算总电表功率
        Integer meterPower = calculateTotalMeterPower(runningData);
        
        // 使用setter方法设置属性
        Meter meter = new Meter();
        try {
            // 设备信息
            meter.setDeviceId(runningData.getSysSn() + systemInfo.getMeterModel());
            meter.setDeviceTime(runningData.getUploadDatetime());
            
            // 电表功率
            meter.setPowerW(meterPower);
            meter.setReactivePowerVar(0);
            
            // 电网电压和频率
            meter.setGridVoltage1V(runningData.getUA());
            meter.setGridVoltage2V(runningData.getUB());
            meter.setGridVoltage3V(runningData.getUC());
            meter.setGridFrequencyHz(runningData.getFac());
            
            // 累计电网能量
            meter.setCumulativeGridImportWh(runningData.getEInput());
            meter.setCumulativeGridExportWh(runningData.getEOutput());
        } catch (Exception e) {
            log.error("Error setting Meter properties: {}", e.getMessage());
        }
        
        return meter;
    }

    /**
     * 将SystemInfo转换为SiteStaticData
     *
     * @param systemInfo 系统信息
     * @return SiteStaticData
     */
    public SiteStaticData convertToSiteStaticData(SystemInfo systemInfo) {
        SiteStaticData siteStaticData = new SiteStaticData();
        try {
            siteStaticData.setSiteId(systemInfo.getSysSn());
        } catch (Exception e) {
            log.error("Error setting SiteStaticData properties: {}", e.getMessage());
        }
        
        return siteStaticData;
    }

    /**
     * 计算总电表功率
     *
     * @param runningData ByteWatt运行数据
     * @return 总电表功率
     */
    private Integer calculateTotalMeterPower(RunningData runningData) {
        double l1 = runningData.getPMeterL1() != null ? runningData.getPMeterL1() : 0;
        double l2 = runningData.getPMeterL2() != null ? runningData.getPMeterL2() : 0;
        double l3 = runningData.getPMeterL3() != null ? runningData.getPMeterL3() : 0;
        return (int) (l1 + l2 + l3);
    }

    /**
     * 计算总太阳能发电功率
     *
     * @param runningData ByteWatt运行数据
     * @return 总太阳能发电功率
     */
    private Integer calculateTotalSolarPower(RunningData runningData) {
        double pv1 = runningData.getPPv1() != null ? runningData.getPPv1() : 0;
        double pv2 = runningData.getPPv2() != null ? runningData.getPPv2() : 0;
        double pv3 = runningData.getPPv3() != null ? runningData.getPPv3() : 0;
        double pv4 = runningData.getPPv4() != null ? runningData.getPPv4() : 0;
        return (int) (pv1 + pv2 + pv3 + pv4);
    }
}
