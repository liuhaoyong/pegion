package com.github.pigeon.api.sender;

import org.apache.commons.lang3.StringUtils;
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
    
    private static final String SUCCESS_RESULT ="OK";
    private RestTemplate restTemplate;

    public HttpSender(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public EventSendResult send(EventWrapper eventContent, EventSubscriberConfig config) {
        long startTime = System.currentTimeMillis();
        try {
            String result =  restTemplate.postForObject(eventContent.getTargetAddress(), eventContent.getContent(),
                    String.class);
            if(StringUtils.equalsIgnoreCase(SUCCESS_RESULT, StringUtils.trim(result)))
            {
                logger.info("http事件发送成功，耗时={},content={}", startTime-System.currentTimeMillis(), eventContent);
                return EventSendResult.getSuccessResult();
            }else
            {
                logger.info("http事件发送失败，耗时={},response={}, content={}",result, startTime-System.currentTimeMillis(), eventContent);
                return EventSendResult.getFailResult(StringUtils.left(result, 100), true);
            }
        } catch (Throwable e) {
            logger.error("http事件发送异常,耗时[{}],[{}]",System.currentTimeMillis() - startTime, eventContent,  e);
            return EventSendResult.getFailResult(e.getMessage(), true);
        }
    }

}
