package com.neovolt.evergen.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.neovolt.evergen.model.bytewatt.RunningData;
import com.neovolt.evergen.model.bytewatt.SystemInfo;

/**
 * ByteWattService集成测试类
 * 使用Spring Boot测试框架自动注入依赖和配置
 */
@SpringBootTest
public class ByteWattServiceTest {

    @Autowired
    private ByteWattService byteWattService;
    
    /**
     * 测试获取组内设备运行数据
     */
    @Test
    public void testGetGroupRunningData() {
        System.out.println("===== 测试 getGroupRunningData 方法 =====");
        
        // 调用服务方法
        List<RunningData> data = byteWattService.getGroupRunningData();
        
        // 打印结果
        System.out.println("获取到 " + data.size() + " 条运行数据");
        
        if (!data.isEmpty()) {
            RunningData firstData = data.get(0);
            System.out.println("第一条数据详情:");
            System.out.println("基本信息:");
            System.out.println("  设备SN: " + firstData.getSysSn());
            System.out.println("  上传时间: " + firstData.getUploadDatetime());
            
            System.out.println("电池信息:");
            System.out.println("  电池电量(SOC): " + firstData.getSoc() + "%");
            System.out.println("  电池功率: " + firstData.getPBat() + "W");
            System.out.println("  电池电压: " + firstData.getBatV() + "V");
            System.out.println("  电池电流: " + firstData.getBatC() + "A");
            System.out.println("  电池充电能量: " + firstData.getECharge() + "Wh");
            
            System.out.println("光伏信息:");
            System.out.println("  PV1功率: " + firstData.getPPv1() + "W");
            System.out.println("  PV2功率: " + firstData.getPPv2() + "W");
            System.out.println("  PV3功率: " + firstData.getPPv3() + "W");
            System.out.println("  PV4功率: " + firstData.getPPv4() + "W");
            System.out.println("  PV总输入能量: " + firstData.getEpvTotal() + "Wh");
            
            System.out.println("电网信息:");
            System.out.println("  L1市电电压: " + firstData.getUA() + "V");
            System.out.println("  L2市电电压: " + firstData.getUB() + "V");
            System.out.println("  L3市电电压: " + firstData.getUC() + "V");
            System.out.println("  电网频率: " + firstData.getFac() + "Hz");
            
            System.out.println("电表信息:");
            System.out.println("  电表L1功率: " + firstData.getPMeterL1() + "W");
            System.out.println("  电表L2功率: " + firstData.getPMeterL2() + "W");
            System.out.println("  电表L3功率: " + firstData.getPMeterL3() + "W");
            System.out.println("  光伏并网机电表功率: " + firstData.getPMeterDc() + "W");
            System.out.println("  电表入户能量: " + firstData.getEInput() + "Wh");
            System.out.println("  电表出户能量: " + firstData.getEOutput() + "Wh");
            
            System.out.println("其他信息:");
            System.out.println("  逆变器工作模式: " + firstData.getInvWorkMode());
            
            // 计算总功率
            double totalMeterPower = 0;
            if (firstData.getPMeterL1() != null) totalMeterPower += firstData.getPMeterL1();
            if (firstData.getPMeterL2() != null) totalMeterPower += firstData.getPMeterL2();
            if (firstData.getPMeterL3() != null) totalMeterPower += firstData.getPMeterL3();
            
            double totalSolarPower = 0;
            if (firstData.getPPv1() != null) totalSolarPower += firstData.getPPv1();
            if (firstData.getPPv2() != null) totalSolarPower += firstData.getPPv2();
            if (firstData.getPPv3() != null) totalSolarPower += firstData.getPPv3();
            if (firstData.getPPv4() != null) totalSolarPower += firstData.getPPv4();
            
            System.out.println("计算值:");
            System.out.println("  总电表功率: " + totalMeterPower + "W");
            System.out.println("  总光伏功率: " + totalSolarPower + "W");
        } else {
            System.out.println("未获取到任何运行数据，请检查API参数是否正确或者组内是否有设备");
        }
    }
    
    /**
     * 测试获取系统列表
     */
    @Test
    public void testGetSystemList() {
        System.out.println("===== 测试 getSystemList 方法 =====");
        
        // 调用服务方法
        List<SystemInfo> systems = byteWattService.getSystemList();
        
        // 打印结果
        System.out.println("获取到 " + systems.size() + " 个系统");
        
        if (!systems.isEmpty()) {
            System.out.println("系统列表:");
            for (int i = 0; i < systems.size(); i++) {
                SystemInfo system = systems.get(i);
                System.out.println((i + 1) + ". SN: " + system.getSysSn());
                System.out.println("   型号: " + system.getSystemModel());
                System.out.println("   逆变器额定功率: " + system.getPoinv() + "W");
                System.out.println("   电池容量: " + system.getCobat() + "kWh");
                System.out.println("   在线状态: " + (system.getNetWorkStatus() == 1 ? "在线" : "离线"));
                System.out.println("   系统状态: " + system.getState());
                System.out.println();
            }
        } else {
            System.out.println("未获取到任何系统信息，请检查API参数是否正确");
        }
    }
}
