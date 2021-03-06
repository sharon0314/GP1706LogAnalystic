package com.qianfeng.common;

/**
 * @Auther: lyd
 * @Date: 2018/7/27 14:57
 * @Description:kpi的枚举
 */
public enum KpiType {
    NEW_USER("new_user"),
    BROWSER_NEW_USER("browser_new_user");

    public String kpiName;

    KpiType(String kpiName) {
        this.kpiName = kpiName;
    }

    /**
     * 根据kpi的name获取kpi的枚举
     * @param kpiName
     * @return
     */
    public KpiType valueOfType(String kpiName){
        for (KpiType kpi : values()){
            if(kpiName.equals(kpi.kpiName)){
                return kpi;
            }
        }
        throw  new RuntimeException("该kpiName暂不支持获取kpi的枚举."+kpiName);
    }
}
