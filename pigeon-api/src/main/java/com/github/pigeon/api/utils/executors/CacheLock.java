package com.github.pigeon.api.utils.executors;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

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
public class CacheLock  implements CLock {

    /**
     * redis模板
     */
    private StringRedisTemplate template;
    
    /**
     * 锁名称
     */
    private String lockName;
    
    public CacheLock(StringRedisTemplate template, java.lang.String lockName) {
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
                long time = Integer.MAX_VALUE;
                if(leaseTime > 0){
                    if(leaseTime > Integer.MAX_VALUE){
                        time = Integer.MAX_VALUE;
                    }else{
                        time = unit.toSeconds(leaseTime);
                    }
                }
                Jedis jedis = (Jedis)connection.getNativeConnection();
                return jedis.set(lockName.getBytes(), Thread.currentThread().getName().getBytes(), "NX".getBytes(), "EX".getBytes(), time);
            }
   
        });
        if("OK".equalsIgnoreCase(result)){
            return true;
        }
        return false;
    }

    @Override
    public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        boolean result = tryAcquire(leaseTime, unit);
        if(result){
            return true;
        }

        final long deadline = DateUtil.getCurrentTimeMillis() + unit.toMillis(waitTime);
        long timeout = deadline;
        long sleepTime = 3;//初始3毫秒
        long roopCount = 0;
        for(;;){
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
            TimeUnit.MILLISECONDS.sleep(sleepTime);

            timeout = deadline - DateUtil.getCurrentTimeMillis();
            if(timeout <= 0L){
                return false;
            }
            if (Thread.interrupted()){
                throw new InterruptedException();
            }
        }

    }

    @Override
    public void lock(long leaseTime, TimeUnit unit) {
        try {
            lockInterruptibly(leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isLocked() {
        return template.hasKey(lockName);
    }

    @Override
    public void lock() {
        lock(-1,null);
    }

    @Override
    public void lockInterruptibly(long leaseTime, TimeUnit unit) throws InterruptedException{
        if (Thread.interrupted())
            throw new InterruptedException();
        boolean result = tryAcquire(leaseTime, unit);
        if(!result){
            throw new InterruptedException();
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        lockInterruptibly(-1,null);
    }

    @Override
    public boolean tryLock() {
        return tryAcquire(-1,null);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return tryAcquire(time,unit);
    }

    @Override
    public void unlock() {
        delete();
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }


    /**
     * @return
     */
    public void delete() {
        template.delete(lockName);
    }

}
