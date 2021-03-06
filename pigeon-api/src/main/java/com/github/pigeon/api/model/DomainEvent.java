package com.github.pigeon.api.model;


import java.io.Serializable;

/**
 * 领域事件
 *
 * @author liuhaoyong
 *         time : 2015年11月2日 下午8:19:07
 */
public interface DomainEvent extends Serializable {
    /**
     * 返回该事件标识, 此标识应唯一
     *
     * @return
     */
    String getEventKey();
    
    /**
     * 关联日志MDC：同一MDC关联查询整个事件处理过程
     *
     * @return
     */
    String getMdcKey();
}
