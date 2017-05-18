package com.github.pigeon.api.sender;


import com.github.pigeon.api.model.EventSendResult;
import com.github.pigeon.api.model.EventSubscriberConfig;
import com.github.pigeon.api.model.EventWrapper;

/**
 * 事件发送器
 *
 * @author liuhaoyong
 *         time : 2014-5-14 下午1:11:42
 */
public interface EventSender {

    /**
     * 发送事件
     *
     * @param eventContent
     * @return
     */
    EventSendResult send(EventWrapper eventContent, EventSubscriberConfig config);
}