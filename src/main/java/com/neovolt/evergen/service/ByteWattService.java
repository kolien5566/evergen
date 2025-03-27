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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovolt.evergen.model.bytewatt.ByteWattResponse;
import com.neovolt.evergen.model.bytewatt.RunningData;
import com.neovolt.evergen.model.bytewatt.SystemInfo;
import com.neovolt.evergen.model.site.HybridInverter;
import com.neovolt.evergen.model.site.Meter;
import com.neovolt.evergen.model.site.SiteStaticData;
import com.neovolt.evergen.util.ByteWattUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * ByteWatt服务类，提供API调用和数据转换功能
 */
@Service
@Slf4j
public class ByteWattService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${bytewatt.api.base-url}")
    private String baseUrl;

    @Value("${bytewatt.api.api-key}")
    private String apiKey;

    @Value("${bytewatt.api.key}")
    private String key;

    @Value("${bytewatt.api.group-key}")
    private String groupKey;

    public ByteWattService(ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
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
            
            ResponseEntity<ByteWattResponse<List<SystemInfo>>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<ByteWattResponse<List<SystemInfo>>>() {}
            );
            
            return response.getBody() != null ? response.getBody().getData() : new ArrayList<>();
        } catch (Exception e) {
            log.error("Error getting system list: {}", e.getMessage());
            return new ArrayList<>();
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
            String checkCode = ByteWattUtils.generateSnPassword(serialNumber);
            
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
        
        // 创建HybridInverter对象并设置属性
        HybridInverter inverter = new HybridInverter();
        // 使用反射设置属性，避免直接调用setter方法
        try {
            java.lang.reflect.Field deviceIdField = HybridInverter.class.getDeclaredField("deviceId");
            deviceIdField.setAccessible(true);
            deviceIdField.set(inverter, runningData.getSysSn());
            
            java.lang.reflect.Field deviceTimeField = HybridInverter.class.getDeclaredField("deviceTime");
            deviceTimeField.setAccessible(true);
            deviceTimeField.set(inverter, runningData.getUploadDatetime());
            
            java.lang.reflect.Field batteryPowerField = HybridInverter.class.getDeclaredField("batteryPowerW");
            batteryPowerField.setAccessible(true);
            batteryPowerField.set(inverter, batteryPower);
            
            java.lang.reflect.Field meterPowerField = HybridInverter.class.getDeclaredField("meterPowerW");
            meterPowerField.setAccessible(true);
            meterPowerField.set(inverter, meterPower);
            
            java.lang.reflect.Field solarPowerField = HybridInverter.class.getDeclaredField("solarPowerW");
            solarPowerField.setAccessible(true);
            solarPowerField.set(inverter, solarPower);
            
            // 设置其他属性...
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
    public Meter convertToMeter(RunningData runningData) {
        // 计算总电表功率
        Integer meterPower = calculateTotalMeterPower(runningData);
        
        // 创建Meter对象并设置属性
        Meter meter = new Meter();
        // 使用反射设置属性，避免直接调用setter方法
        try {
            java.lang.reflect.Field deviceIdField = Meter.class.getDeclaredField("deviceId");
            deviceIdField.setAccessible(true);
            deviceIdField.set(meter, runningData.getSysSn() + "-meter");
            
            java.lang.reflect.Field deviceTimeField = Meter.class.getDeclaredField("deviceTime");
            deviceTimeField.setAccessible(true);
            deviceTimeField.set(meter, runningData.getUploadDatetime());
            
            java.lang.reflect.Field powerField = Meter.class.getDeclaredField("powerW");
            powerField.setAccessible(true);
            powerField.set(meter, meterPower);
            
            // 设置其他属性...
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
        // 使用反射设置属性，避免直接调用setter方法
        try {
            java.lang.reflect.Field siteIdField = SiteStaticData.class.getDeclaredField("siteId");
            siteIdField.setAccessible(true);
            siteIdField.set(siteStaticData, systemInfo.getSysSn());
            
            // 设置其他属性...
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
