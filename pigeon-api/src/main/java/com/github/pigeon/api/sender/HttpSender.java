package com.github.pigeon.api.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import com.github.pigeon.api.model.EventSendResult;
import com.github.pigeon.api.model.EventSubscriberConfig;
import com.github.pigeon.api.model.EventWrapper;

/**
 * http协议的事件发送器
 * 
 * @author liuhaoyong 2017年5月16日 下午5:42:48
 */
public class HttpSender implements EventSender {

    private static final Logger logger = LoggerFactory.getLogger(HttpSender.class);
    
    private RestTemplate restTemplate;

    public HttpSender(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public EventSendResult send(EventWrapper eventContent, EventSubscriberConfig config) {
        long startTime = System.currentTimeMillis();
        try {
            EventSendResult result =  restTemplate.postForObject(eventContent.getTargetAddress(), eventContent.getContent(),
                    EventSendResult.class);
            logger.info("http事件发送完成，result={}, 耗时={},content={}",result, startTime-System.currentTimeMillis(), eventContent);
            return result;
        } catch (Throwable e) {
            logger.error("事件发送异常,耗时[{}],[{}]",System.currentTimeMillis() - startTime, eventContent,  e);
            return EventSendResult.getFailResult(e.getMessage(), true);
        }
    }

}
