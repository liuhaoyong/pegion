package com.github.pigeon.api.sender;

import java.util.UUID;

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

    private static final int    RESULT_MAX_LEN = 200;
    public static final String  SUCCESS_RESULT = "OK";
    private static final Logger logger         = LoggerFactory.getLogger(HttpSender.class);

    private CloseableHttpClient httpClient;

    public HttpSender(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public EventSendResult send(EventWrapper eventContent, EventSubscriberConfig config) {

        if (StringUtils.isBlank(eventContent.getContent())) {
            logger.warn("发送的事件内容为空,忽略");
            return EventSendResult.getSuccessResult();
        }

        long startTime = System.currentTimeMillis();
        try {

            String result = doPost(eventContent.getTargetAddress(), eventContent.getContent());
            boolean isSuccess = isSuccess(config, result);

            if (isSuccess) {
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

    /**
     * @param config
     * @param result
     * @return
     */
    private boolean isSuccess(EventSubscriberConfig config, String result) {

        //如果返回结果字符串为空，或者大于１００个字符，失败
        if (StringUtils.isBlank(result) || StringUtils.length(result) > RESULT_MAX_LEN) {
            return false;
        }

        //先去掉特殊字符，目前仅去掉双引号和首尾的空格，兼容财务系统返回带双引号的OK的情况
        result = StringUtils.remove(result, "\"").trim();

        //如果等于默认的ＯＫ,  返回成功
        if (StringUtils.equalsIgnoreCase(result, SUCCESS_RESULT)) {
            return true;
        }

        //如果事件订阅配置里配置了特定成功字符串，则判断返回的结果是否包含该字符串
        //不用等于，用包含是为了兼容网站返回一个大的ｊｓｏｎ串的情况
        if (StringUtils.isNotBlank(config.getSuccessString())
                && StringUtils.contains(result, config.getSuccessString())) {
            return true;
        }
        return false;
    }

    public String doPost(String url, String content) throws Exception {

        // 创建Httpclient对象
        CloseableHttpResponse response = null;

        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader("Content-Type", "application/json");
            //加一个requestID, .net接收端有时需要用该值来作为唯一标识搜索日志
            httpPost.addHeader("RequestID", StringUtils.replace(UUID.randomUUID().toString(), "-", ""));
            httpPost.setEntity(new StringEntity(content, "UTF-8"));
            response = httpClient.execute(httpPost);
            String resultString = EntityUtils.toString(response.getEntity(), "utf-8");
            logger.info("http sender result, statusLine={}, body={}", response.getStatusLine(), resultString);
            return resultString;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

}
