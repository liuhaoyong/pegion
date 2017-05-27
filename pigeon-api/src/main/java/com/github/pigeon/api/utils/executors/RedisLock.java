package com.github.pigeon.api.utils.executors;

import java.util.concurrent.TimeUnit;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.github.pigeon.api.utils.DateUtil;

import redis.clients.jedis.Jedis;

/**
 * 基于redis的锁实现
 * 
 * @author liuhaoyong 2017年5月12日 下午3:55:24
 */
public class RedisLock  implements DistributedLock {

    /**
     * redis模板
     */
    private StringRedisTemplate template;
    
    /**
     * 锁名称
     */
    private String lockName;
    
    public RedisLock(StringRedisTemplate template, java.lang.String lockName) {
        this.lockName = lockName;
        this.template = template;
        
    }

    /**
     *
     * @param waitTime
     * @param leaseTime
     * @param unit
     * @return
     */
    private boolean tryAcquire(final long leaseTime, TimeUnit unit){
        String result  =  template.opsForValue().getOperations().execute(new RedisCallback<String>()
        {
            @Override
            public String doInRedis(RedisConnection connection) throws DataAccessException {
                long time = unit.toSeconds(leaseTime);
                Jedis jedis = (Jedis)connection.getNativeConnection();
                
                /**
                 * nx 表示key不存在时才能set成功
                 * ex 表示过期时间的单位是秒
                 */
                return jedis.set(lockName.getBytes(), Thread.currentThread().getName().getBytes(), "NX".getBytes(), "EX".getBytes(), time);
            }
   
        });
        if("OK".equalsIgnoreCase(result)){
            return true;
        }
        return false;
    }

    @Override
    public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit)  {
        boolean result = tryAcquire(leaseTime, unit);
        if(result){
            return true;
        }

        final long deadline = DateUtil.getCurrentTimeMillis() + unit.toMillis(waitTime);
        long timeout = deadline;
        long sleepTime = 3;//初始3毫秒
        long roopCount = 0;
        while(true){
            roopCount ++ ;
            //保护措施，sleep时间随着循环测试增加，超过一定测试就直接返回获取锁失败
            if(roopCount >= 800){
                return false;
            }else if(roopCount >= 500){
                sleepTime = 800;
            }else if(roopCount >= 100){
                sleepTime = 300;
            }else if(roopCount >= 50){
                sleepTime = 100;
            }else if(roopCount >= 30){
                sleepTime = 50;
            }else if(roopCount >= 10){
                sleepTime = 10;
            }
            if(tryAcquire(leaseTime, unit)){
                return true;
            }
            //适当睡眠，避免对codis过于频繁的访问
            try {
                TimeUnit.MILLISECONDS.sleep(sleepTime);
            } catch (InterruptedException e) {
                return false;
            }

            timeout = deadline - DateUtil.getCurrentTimeMillis();
            if(timeout <= 0L){
                return false;
            }
            if (Thread.interrupted()){
                return false;
            }
        }

    }

    @Override
    public boolean lock(long leaseTime, TimeUnit unit) {
       return tryAcquire(leaseTime, unit);
    }

    @Override
    public boolean isLocked() {
        return template.hasKey(lockName);
    }

    @Override
    public void releaseLock() {
        template.delete(lockName);
    }

}
