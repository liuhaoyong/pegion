package com.github.pigeon.api.repository;

import java.util.TreeMap;

import com.github.pigeon.api.model.EventWrapper;
import com.github.pigeon.api.model.FailedEventLog;

/**
 * 事件仓储服务
 * @author liuhaoyong 2017年5月16日 上午10:16:48
 */
public interface EventRepository {

    
    /**
     * 事件持久化
     * @param event
     * @throws Exception
     */
    public void persistEvent(EventWrapper event)  throws Exception;

    /**
     * 事件删除
     * @param event
     */
    public void delEvent(EventWrapper event)  throws Exception;


    /**
     * 事件删除
     * @param eventUniqueKey
     * @throws Exception
     */
    public void delEvent(String eventUniqueKey)  throws Exception;
    
    /**
     * 从正常事件队列中拿数据
     *
     * @param min
     * @param max
     * @param offset
     * @param count
     * @return
     * @throws Exception
     */
    public TreeMap<String,String> extractEvent(double min, double max, int offset, int count)  throws Exception;

    /**
     * 获取指定区间内正常事件的数量
     *
     * @param min
     * @param max
     * @return
     * @throws Exception
     */
    public long getEventCount(double min, double max)  throws Exception;

    /**
     *
     * @return
     */
    public long getEventCount();
    

    /**
     * 持久化发送异常事件
     *
     * @param event
     * @param score
     */
    public void persistExceptionalEvent(EventWrapper event, long execTime)  throws Exception;

    /**
     * 删除发送异常的事件
     *
     * @param event
     */
    public void delExceptionalEvent(EventWrapper event)  throws Exception;
    
    /**
     * 从异常事件队列中拿数据
     *
     * @param min
     * @param max
     * @param offset
     * @param count
     * @return key->eventContent
     * @throws Exception
     */
    public TreeMap<String,String> extractExceptionalEvent(double min, double max, int offset, int count)  throws Exception;

    /**
     * 获取指定区间内异常事件的数量
     *
     * @param min
     * @param max
     * @return
     */
    public long getExceptionEventCount(double min, double max)  throws Exception;

    /**
     * 获取异常事件的总量
     * @return
     */
    public long getExceptionEventCount();
    
    /**
     * 保存重试多次仍然失败的事件发送日志
     * @param failedEventLog
     */
    public void    saveFailedLog(FailedEventLog failedEventLog);
    

}
