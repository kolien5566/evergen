package com.neovolt.evergen.util;

public class ByteWattUtils {
    
    /**
     * 生成SN密码/校验码
     * 
     * @param sn 系统序列号
     * @return 生成的校验码
     */
    public static String generateSnPassword(String sn) {
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
     * 构建充电参数字符串
     * 
     * @param powerW 充电功率（瓦特）
     * @return 参数字符串
     */
    public static String buildChargeParameter(int powerW) {
        return powerW + "|0|0|0|0|0|0";
    }
    
    /**
     * 构建放电参数字符串
     * 
     * @param powerW 放电功率（瓦特）
     * @return 参数字符串
     */
    public static String buildDischargeParameter(int powerW) {
        return "0|" + powerW + "|0|0|0|0|0";
    }
    
    /**
     * 构建自消费模式参数字符串
     * 
     * @return 参数字符串
     */
    public static String buildSelfConsumptionParameter() {
        return "0|0|0|0|0|0|0";
    }
}
