package com.github.pigeon.api.enums;

/**
 * 事件发布状态
 * @author liuhaoyong
 * time : 2015年11月2日 下午8:56:37
 */
public enum EventPublishStatus {

    PROCESSING("P"),
    
    FAILED("F"),
    
    SUCCESS("S");
    
    private String code;
    
    private  EventPublishStatus(String code)
    {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    
}
