package com.github.pigeon.api.convertor;

import com.alibaba.fastjson.JSON;
import com.github.pigeon.api.model.DomainEvent;
import com.github.pigeon.api.model.EventSubscriberConfig;

/**
 * 针对spring协议的默认的事件转换器
 * 此实现不做任何转换
 * @author liuhaoyong
 * time : 2015年11月3日 上午10:17:08
 */
public class DefaultSpringProtocolConvertor implements EventConvertor<DomainEvent> {


    @Override
    public String convert(DomainEvent event, EventSubscriberConfig config) {
        return JSON.toJSONString(event);
    }

    @Override
    public String getTargetAddress(DomainEvent event, EventSubscriberConfig config) {
        return null;
    }
}
