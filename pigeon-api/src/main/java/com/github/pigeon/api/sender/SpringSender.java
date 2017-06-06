package com.github.pigeon.api.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.github.pigeon.api.listeners.SpringEventListener;
import com.github.pigeon.api.model.EventSendResult;
import com.github.pigeon.api.model.EventSubscriberConfig;
import com.github.pigeon.api.model.EventWrapper;
import com.github.pigeon.api.utils.PigeonUtils;


/**
 * spring协议的事件发送器
 *
 * @author liuhaoyong
 *         time : 2014-1-16 下午6:44:18
 */
public class SpringSender implements EventSender {

    private static final Logger logger = LoggerFactory.getLogger(SpringSender.class);

    private ApplicationContext applicationContext;
    
    public SpringSender(ApplicationContext applicationContext)
    {
        this.applicationContext = applicationContext;
    }

    @Override
    public EventSendResult send(EventWrapper eventContent, EventSubscriberConfig config) {
        Object o = PigeonUtils.getBean(applicationContext,eventContent.getTargetAddress());
        if (o == null) {
            String message = "No " + SpringEventListener.class.getName() + " bean defined: " + eventContent.getTargetAddress();
            logger.error(message);
            return EventSendResult.getFailResult(message, false);
        }

        if (!(o instanceof SpringEventListener)) {
            String message = eventContent.getTargetAddress() + " bean is expected as " + SpringEventListener.class.getName()
                    + ", but get " + o.getClass().getName();
            logger.error(message);
            return EventSendResult.getFailResult(message, false);
        }

        try {
            return ((SpringEventListener) o).handleEvent(eventContent.getContent());
        } catch (Exception e) {
            logger.error("[PigeonEvent]Faied to handle event by {} bean]", eventContent.getTargetAddress(), e);
            return EventSendResult.getFailResult("未知异常:" + e.getMessage()+"-->"+ PigeonUtils.printErrorTrace(e), false);
        }
    }

}