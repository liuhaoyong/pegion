package com.github.pigeon.api.repository.impl;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.github.pigeon.api.model.EventWrapper;
import com.github.pigeon.api.model.FailedEventLog;
import com.github.pigeon.api.repository.EventRepository;
import com.github.pigeon.api.utils.PigeonUtils;

/**
 * 基于redis存储的事件仓储实现
 * 
 * @author liuhaoyong 2017年5月16日 上午10:22:01
 */
public class RedisEventRepository  implements EventRepository {

    private static final Logger logger = LoggerFactory.getLogger(RedisEventRepository.class);
    private static final Logger failedEventlogger = LoggerFactory.getLogger("failed.event.logger");
    
    private final static double defaultScore = 0;
   

    private StringRedisTemplate               redisTemplate;

    /**
     * 保存初始事件的队列
     */
    private BoundZSetOperations<String, String>         normalQueue;
    private BoundHashOperations<String, String, String> normalHashMap;
    
    /**
     * 保存异常情况下重试事件的队列
     */
    private BoundHashOperations<String, String, String> retryHashMap;
    private BoundZSetOperations<String, String>         retryQueue;
    
    public  RedisEventRepository(PigeonConfigProperties params, StringRedisTemplate template)
    {
        this.redisTemplate = template;
        normalQueue = redisTemplate.boundZSetOps(params.getRedisNormalQueue());
        normalHashMap = redisTemplate.boundHashOps(params.getRedisNormalHashMap());
        retryHashMap = redisTemplate.boundHashOps(params.getRedisRetryHashMap());
        retryQueue = redisTemplate.boundZSetOps(params.getRedisRetryQueue());
    }

    
    /**
     * 消费队列
     *
     * @param event
     */
    private void redisOps(final Event event) throws Exception {
        String eventKey = event.getEvent().genUniformEventKey();
        if (event.getOpr() == OprTypeEnum.persistEvent) {
            normalQueue.add(eventKey, event.getScore());
            normalHashMap.put(eventKey, PigeonUtils.marshall(event.getEvent()));
        } else if (event.getOpr() == OprTypeEnum.delEvent) {
            normalQueue.remove(eventKey);
            normalHashMap.delete(eventKey);
        } else if (event.getOpr() == OprTypeEnum.persistExceptionEvent) {
            retryQueue.add(eventKey, event.getScore());
            retryHashMap.put(eventKey, PigeonUtils.marshall(event.getEvent()));
        } else if (event.getOpr() == OprTypeEnum.delExceptionEvent) {
            retryQueue.remove(eventKey);
            retryHashMap.delete(eventKey);
        }
    }


    /**
     * 有新的事件发布时
     *
     * @param event
     */
    public void persistEvent(EventWrapper event) {
        doCommond(event, OprTypeEnum.persistEvent);
    }

    /**
     * 事件处理成功后删除事件
     *
     * @param event
     */
    public void delEvent(EventWrapper event) {
        doCommond(event, OprTypeEnum.delEvent);
    }

    /**
     *
     * @param eventUniqueKey
     */
    public void delEvent(String eventUniqueKey){
        normalQueue.remove(eventUniqueKey);
        normalHashMap.delete(eventUniqueKey);
    }

    /**
     * 保存异常事件
     *
     * @param event
     * @param score
     */
    public void persistExceptionalEvent(EventWrapper event, long execTime) {
        doCommond(event, execTime, OprTypeEnum.persistExceptionEvent);
    }

    /**
     * 删除异常事件
     *
     * @param event
     */
    public void delExceptionalEvent(EventWrapper event) {
        doCommond(event, OprTypeEnum.delExceptionEvent);
    }

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
    public TreeMap<String,String> extractExceptionalEvent(double min, double max, int offset, int count) throws Exception {
        return this.getEventList(retryQueue, retryHashMap, min, max, offset, count);
    }

    /**
     * 获取指定区间内异常事件的数量
     *
     * @param min
     * @param max
     * @return
     */
    public long getExceptionEventCount(double min, double max) throws Exception {
        return retryQueue.count(min, max);
    }

    /**
     * @return
     */
    @Override
    public long getExceptionEventCount() {
        return retryQueue.size();
    }

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
    public TreeMap<String,String> extractEvent(double min, double max, int offset, int count) throws Exception {
        return getEventList(this.normalQueue,this.normalHashMap, min,max,offset,count);  
    }
    
    
    /**
     * 获取事件列表
     * @param set
     * @param map
     * @param min
     * @param max
     * @param offset
     * @param count
     * @return
     */
    private TreeMap<String,String> getEventList(final BoundZSetOperations<String,String> set,BoundHashOperations<String, String, String> map,  double min, double max, int offset, int count)
    {
        List<String> keys  =  set.getOperations().execute(new RedisCallback<List<String>>()
        {

            @Override
            public List<String> doInRedis(RedisConnection connection) throws DataAccessException {
                Set<byte[]>  response = connection.zRangeByScore(set.getKey().getBytes(), min, max, offset, count);
                List<String> result = new ArrayList<String>();
                for(byte[] item: response)
                {
                    try {
                        result.add(new String(item,"utf-8"));
                    } catch (UnsupportedEncodingException e) {
                        logger.error("不支持的编码方式");
                    }
                }
                return result;
            }
   
        });
        
        if (keys == null || keys.isEmpty()) {
            return null;
        }
        
        List<String> values = map.multiGet(keys);
        TreeMap<String,String> result = new TreeMap<String,String>();
        for(int i=0; i<keys.size(); i++)
        {
            if(StringUtils.isNotBlank(values.get(i)))
            {
                result.put(keys.get(i), values.get(i));
            }
        }
        return result;
    }

    /**
     * 获取指定区间内正常事件的数量
     *
     * @param min
     * @param max
     * @return
     * @throws Exception
     */
    public long getEventCount(double min, double max) throws Exception {
        return normalQueue.count(min, max);
    }

    /**
     * @return
     */
    @Override
    public long getEventCount() {
        return normalQueue.size();
    }

    /**
     *
     */
    public static enum OprTypeEnum {
        persistEvent, delEvent, persistExceptionEvent, delExceptionEvent;
    }

    /**
     *
     */
    private class Event implements java.io.Serializable {

        private static final long serialVersionUID = 1322689566970457037L;
        private EventWrapper event;
        private OprTypeEnum opr;
        private double score;


        public Event(EventWrapper event, OprTypeEnum opr, double score) {
            this.event = event;
            this.opr = opr;
            this.score = score;
        }

        public EventWrapper getEvent() {
            return event;
        }


        public OprTypeEnum getOpr() {
            return opr;
        }


        public double getScore() {
            return score;
        }

    }

    /**
     * 将事件放入队列
     *
     * @param event
     */
    public void doCommond(EventWrapper event, OprTypeEnum opr) {
        doCommond(event, System.currentTimeMillis(), opr);
    }

    public void doCommond(EventWrapper event, long execTime, OprTypeEnum opr) {
        try {
            if (event != null && opr != null) {
                redisOps(new Event(event, opr, execTime));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void saveFailedLog(FailedEventLog failedEventLog) {
        failedEventlogger.error(failedEventLog.toString());
    }

}
