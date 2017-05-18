package com.github.pigeon.api.model;

import java.io.Serializable;

/**
 * 事件发送结果
 * 
 * @author liuhaoyong
 * time : 2014-4-30 下午4:24:19
 */
public class EventSendResult implements Serializable{

    private static final long serialVersionUID = 2873712356690438911L;
    /**
     * 结果消息
     */
    private String resultMsg;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 能否重试
     */
    private boolean canRetry = false;

    public static EventSendResult getFailResult(String resultMsg, boolean canRetry) {
        EventSendResult result = new EventSendResult();
        result.success = false;
        result.resultMsg = resultMsg;
        result.canRetry = canRetry;
        return result;
    }

    public static EventSendResult getSuccessResult() {
        EventSendResult result = new EventSendResult();
        result.success = true;
        return result;
    }

    public static EventSendResult getSuccessResult(String resultMsg) {
        EventSendResult result = new EventSendResult();
        result.resultMsg = resultMsg;
        result.success = true;
        return result;
    }

    private EventSendResult() {
    };

    public String getResultMsg() {
        return resultMsg;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isCanRetry() {
        return canRetry;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("EventSendResult{");
        sb.append("resultMsg='").append(resultMsg).append('\'');
        sb.append(", success=").append(success);
        sb.append(", canRetry=").append(canRetry);
        sb.append('}');
        return sb.toString();
    }
}
