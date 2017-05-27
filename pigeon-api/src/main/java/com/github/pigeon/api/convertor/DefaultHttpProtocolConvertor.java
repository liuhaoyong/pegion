package com.github.pigeon.api.convertor;


import com.alibaba.fastjson.JSON;
import com.github.pigeon.api.model.DomainEvent;
import com.github.pigeon.api.model.EventSubscriberConfig;

/**
 * http协议转换器
 * 将事件转换成json格式的字符串
 * @author liuhaoyong
 * time : 2015年11月3日 下午8:50:03
 */
public class DefaultHttpProtocolConvertor implements EventConvertor<String, DomainEvent> {

    @Override
    public  String convert(DomainEvent event, EventSubscriberConfig config){
        return JSON.toJSONString(event);
    }
    
    @Override
    public String getTargetAddress(DomainEvent event, EventSubscriberConfig config) {
        return null;
    }
}
