    package com.github.pigeon.api.enums;

/**
 *
 * @author zhuyingzi
 */
public enum EventNotifyStatus {

    INIT("I"), //初始
    WAITING("W"), //待执行
    PROCESSING("P"), //处理中
    SUCCESS("S"), //成功
    FAIL("F"), //失败
    ;

    public String code;

    EventNotifyStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
