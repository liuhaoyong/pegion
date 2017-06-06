package com.github.pigeon.api.listeners;



import com.github.pigeon.api.model.EventSendResult;

/**
 * spring协议件的监听器
 *
 * @author liuhaoyong
 */
public interface SpringEventListener {

    /**
     * 本地处理事件消息
     *
     * @param event
     * @return
     */
    EventSendResult handleEvent(String event);

}
