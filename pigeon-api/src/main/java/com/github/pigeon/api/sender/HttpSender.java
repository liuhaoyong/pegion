package com.github.pigeon.api.sender;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.pigeon.api.model.EventSendResult;
import com.github.pigeon.api.model.EventSubscriberConfig;
import com.github.pigeon.api.model.EventWrapper;

/**
 * http协议的事件发送器
 * 
 * @author liuhaoyong 2017年5月16日 下午5:42:48
 */
public class HttpSender implements EventSender {

    private static final Logger logger         = LoggerFactory.getLogger(HttpSender.class);

    private static final String SUCCESS_RESULT = "OK";
    private CloseableHttpClient httpClient;

    public HttpSender(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public EventSendResult send(EventWrapper eventContent, EventSubscriberConfig config) {
        
        if(StringUtils.isBlank(eventContent.getContent()))
        {
            logger.warn("发送的事件内容为空,忽略");
            return EventSendResult.getSuccessResult();
        }
        
        long startTime = System.currentTimeMillis();
        try {
            String result = doPost(eventContent.getTargetAddress(), eventContent.getContent());
            if (StringUtils.equalsIgnoreCase(SUCCESS_RESULT, StringUtils.trim(result))) {
                logger.info("http事件发送成功，耗时={},content={}", System.currentTimeMillis() - startTime, eventContent);
                return EventSendResult.getSuccessResult();
            } else {
                logger.info("http事件发送失败，耗时={},response={}, content={}", System.currentTimeMillis() - startTime, result,
                        eventContent);
                return EventSendResult.getFailResult(StringUtils.left(result, 100), true);
            }
        } catch (Throwable e) {
            logger.error("http事件发送异常,耗时[{}],[{}]", System.currentTimeMillis() - startTime, eventContent, e);
            return EventSendResult.getFailResult(StringUtils.left(e.getMessage(), 100), true);
        }
    }

    public String doPost(String url, String content) throws Exception {
        
        // 创建Httpclient对象
        CloseableHttpResponse response = null;
        
        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(content, "UTF-8"));
            response = httpClient.execute(httpPost);
            String resultString = EntityUtils.toString(response.getEntity(), "utf-8");
            logger.info("http sender result, statusLine={}, body={}", response.getStatusLine(), resultString);
            return resultString;
        }
        finally {
            if(response!=null)
            {
                response.close();
            }
        }
    }

}
