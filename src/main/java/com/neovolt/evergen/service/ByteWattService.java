package com.neovolt.evergen.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

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
import com.neovolt.evergen.model.bytewatt.SecondLevelData;
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

    // 添加错误代码和错误信息的记录
    private int lastErrorCode = 0;
    private String lastErrorInfo = "";

    public ByteWattService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 获取最后一次API调用的错误代码
     * 
     * @return 错误代码
     */
    public int getLastErrorCode() {
        return this.lastErrorCode;
    }

    /**
     * 获取最后一次API调用的错误信息
     * 
     * @return 错误信息
     */
    public String getLastErrorInfo() {
        return this.lastErrorInfo;
    }

    /**
     * 重置错误状态
     */
    private void resetError() {
        this.lastErrorCode = 0;
        this.lastErrorInfo = "";
    }

    /**
     * 设置错误状态
     * 
     * @param code 错误代码
     * @param info 错误信息
     */
    private void setError(int code, String info) {
        this.lastErrorCode = code;
        this.lastErrorInfo = info;
        log.debug("API错误: 代码={}, 信息={}", code, info);
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
            requestBody.put("page_size", 1000);  // 获取足够多的系统信息
            
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
        // 重置错误状态
        resetError();
        
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
            
            ByteWattResponse responseBody = response.getBody();
            if (responseBody != null) {
                if (responseBody.getCode() == 200) {
                    return true;
                } else {
                    // 记录错误信息
                    setError(responseBody.getCode(), responseBody.getInfo());
                    log.warn("绑定SN失败: 序列号={}, 错误代码={}, 错误信息={}", 
                             serialNumber, responseBody.getCode(), responseBody.getInfo());
                    return false;
                }
            } else {
                setError(-1, "API返回空响应");
                return false;
            }
        } catch (Exception e) {
            setError(-2, e.getMessage());
            log.error("绑定SN异常: {}", e.getMessage(), e);
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
        // 重置错误状态
        resetError();
        
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
            
            ByteWattResponse responseBody = response.getBody();
            if (responseBody != null) {
                if (responseBody.getCode() == 200) {
                    return true;
                } else {
                    // 记录错误信息
                    setError(responseBody.getCode(), responseBody.getInfo());
                    log.warn("添加SN到组失败: 序列号={}, 错误代码={}, 错误信息={}", 
                             serialNumber, responseBody.getCode(), responseBody.getInfo());
                    return false;
                }
            } else {
                setError(-1, "API返回空响应");
                return false;
            }
        } catch (Exception e) {
            setError(-2, e.getMessage());
            log.error("添加SN到组异常: {}", e.getMessage(), e);
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
            inverter.setCumulativeBatteryChargeEnergyWh(runningData.getECharge() * 1000);
            inverter.setCumulativeBatteryDischargeEnergyWh(runningData.getEInput() * 1000);
            inverter.setCumulativePvGenerationWh(runningData.getEpvTotal() * 1000);
            inverter.setCumulativeGridImportWh(runningData.getEInput() * 1000);
            inverter.setCumulativeGridExportWh(runningData.getEOutput() * 1000);
            
            // 电池状态 - 确保值在0-1范围内
            Double soc = runningData.getSoc();
            if (soc != null) {
                soc = soc / 100.0;
                inverter.setStateOfCharge(soc);
            } else {
                inverter.setStateOfCharge(0.0);
            }
            
            // SOH设置为1.0（100%健康度的小数形式）
            inverter.setStateOfHealth(1.0);
            
            // 最大充放电功率
            if (systemInfo != null) {
                inverter.setMaxChargePowerW(systemInfo.getPoinv() != null ? systemInfo.getPoinv().intValue() * 1000 : null);
                inverter.setMaxDischargePowerW(systemInfo.getPoinv() != null ? systemInfo.getPoinv().intValue() * 1000 : null);
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
            String rawDeviceId = runningData.getSysSn() + systemInfo.getMeterModel();
            meter.setDeviceId(rawDeviceId.replaceAll("[^\\w.\\-]", ""));
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
            meter.setCumulativeGridImportWh(runningData.getEInput() * 1000);
            meter.setCumulativeGridExportWh(runningData.getEOutput() * 1000);
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
        double pdc = runningData.getPMeterDc() != null ? runningData.getPMeterDc() : 0;
        return (int) (pv1 + pv2 + pv3 + pv4 + pdc);
    }

    /**
     * 将设备本地时间转换为UTC时间
     * 
     * @param localTime 设备本地时间
     * @param timezoneStr 时区字符串，格式如"+10:00"
     * @return UTC时间
     */
    public LocalDateTime convertToUtcTime(LocalDateTime localTime, String timezoneStr) {
        if (localTime == null || timezoneStr == null) {
            return localTime;
        }
        
        try {
            // 处理timezone格式，确保有+/-前缀
            if (timezoneStr.matches("\\d+:\\d+")) {
                // 如果只有数字和冒号(如"00:00")，则加上"+"前缀
                timezoneStr = "+" + timezoneStr;
            }
            
            // 创建时区偏移对象
            ZoneOffset zoneOffset = ZoneOffset.of(timezoneStr);
            
            // 创建带时区的日期时间
            ZonedDateTime zonedDateTime = localTime.atZone(zoneOffset);
            
            // 转换为UTC时间
            ZonedDateTime utcTime = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC);
            
            return utcTime.toLocalDateTime();
        } catch (Exception e) {
            log.error("Error converting to UTC time. Using local time instead. Error: {}", e.getMessage());
            return localTime;
        }
    }
    /**
     * 使用SecondLevelData中的准确数据更新RunningData
     * 
     * @param runningData 原始运行数据
     * @param secondLevelData 实时功率数据
     * @return 更新后的RunningData对象
     */
    public RunningData mergeWithSecondLevelData(RunningData runningData, SecondLevelData secondLevelData) {
        if (runningData == null || secondLevelData == null) {
            return runningData;
        }
        
        // 创建一个新的RunningData对象，避免修改原对象
        RunningData mergedData = new RunningData();
        
        // 复制原有数据
        mergedData.setSysSn(runningData.getSysSn());
        mergedData.setUA(runningData.getUA());
        mergedData.setUB(runningData.getUB());
        mergedData.setUC(runningData.getUC());
        mergedData.setFac(runningData.getFac());
        mergedData.setBatV(runningData.getBatV());
        mergedData.setBatC(runningData.getBatC());
        mergedData.setInvWorkMode(runningData.getInvWorkMode());
        mergedData.setEpvTotal(runningData.getEpvTotal());
        mergedData.setEInput(runningData.getEInput());
        mergedData.setEOutput(runningData.getEOutput());
        mergedData.setECharge(runningData.getECharge());
        
        // 使用SecondLevelData中的准确数据替换对应字段
        mergedData.setUploadDatetime(secondLevelData.getUploadtime());
        // PV功率数据
        mergedData.setPPv1(secondLevelData.getPpv1() != null ? secondLevelData.getPpv1() : runningData.getPPv1());
        mergedData.setPPv2(secondLevelData.getPpv2() != null ? secondLevelData.getPpv2() : runningData.getPPv2());
        mergedData.setPPv3(secondLevelData.getPpv3() != null ? secondLevelData.getPpv3() : runningData.getPPv3());
        mergedData.setPPv4(secondLevelData.getPpv4() != null ? secondLevelData.getPpv4() : runningData.getPPv4());
        
        // 电表功率数据
        mergedData.setPMeterL1(secondLevelData.getPmeterL1() != null ? secondLevelData.getPmeterL1() : runningData.getPMeterL1());
        mergedData.setPMeterL2(secondLevelData.getPmeterL2() != null ? secondLevelData.getPmeterL2() : runningData.getPMeterL2());
        mergedData.setPMeterL3(secondLevelData.getPmeterL3() != null ? secondLevelData.getPmeterL3() : runningData.getPMeterL3());
        mergedData.setPMeterDc(secondLevelData.getPmeterDc() != null ? secondLevelData.getPmeterDc() : runningData.getPMeterDc());
        
        // 电池相关数据
        mergedData.setPBat(secondLevelData.getPbat() != null ? secondLevelData.getPbat() : runningData.getPBat());
        mergedData.setSoc(secondLevelData.getSoc() != null ? secondLevelData.getSoc() : runningData.getSoc());
        
        return mergedData;
    }

    /**
     * 获取组内所有设备的实时功率数据（SecondLevelData）
     *
     * @return SecondLevelData列表
     */
    public List<SecondLevelData> getGroupSecondLevelData() {
        try {
            Map<String, Object> requestBody = createAuthParams();
            requestBody.put("group_key", groupKey);

            String url = baseUrl + "/Open/Group/GetLastPowerDataByGroup";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<ByteWattResponse<List<SecondLevelData>>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<ByteWattResponse<List<SecondLevelData>>>() {}
            );
            
            ByteWattResponse<List<SecondLevelData>> responseBody = response.getBody();
            if (responseBody != null && responseBody.getCode() == 200) {
                return responseBody.getData() != null ? responseBody.getData() : new ArrayList<>();
            } else {
                log.warn("获取组SecondLevelData失败: 错误代码={}, 错误信息={}", 
                         responseBody != null ? responseBody.getCode() : -1, 
                         responseBody != null ? responseBody.getInfo() : "响应为空");
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("Error getting group second level data: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 获取增强的组运行数据（合并RunningData和SecondLevelData）
     * 
     * @return 合并后的RunningData列表
     */
    public List<RunningData> getEnhancedGroupRunningData() {
        // 获取基础运行数据
        List<RunningData> runningDataList = getGroupRunningData();
        
        // 获取组实时功率数据
        List<SecondLevelData> secondLevelDataList = getGroupSecondLevelData();
        
        // 创建增强数据列表
        List<RunningData> enhancedDataList = new ArrayList<>();
        
        // 为每个运行数据找到对应的实时功率数据并合并
        for (RunningData runningData : runningDataList) {
            SecondLevelData matchingSecondLevel = null;
            
            // 查找匹配的SecondLevelData
            for (SecondLevelData secondLevel : secondLevelDataList) {
                if (runningData.getSysSn().equals(secondLevel.getSysSn())) {
                    matchingSecondLevel = secondLevel;
                    break;
                }
            }
            
            // 合并数据
            RunningData enhancedData = mergeWithSecondLevelData(runningData, matchingSecondLevel);
            enhancedDataList.add(enhancedData);
        }
        
        return enhancedDataList;
    }
}
