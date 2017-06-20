package com.github.pigeon.api.utils.executors;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 互斥任务执行 利用分布式锁，保证同一个任务同一时刻只能被一个线程执行
 * 
 * @author liuhaoyong on 2016/12/13.
 */
public class MutexTaskExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MutexTaskExecutor.class);

    /**
     * 执行互斥任务
     * 
     * @param redisTemplate redis视力
     * @param taskName 任务名称
     * @param taskTimeoutSeconds 任务的最大执行时间
     * @param runner 任务实现
     */
    public static void execute(StringRedisTemplate redisTemplate, String taskName, long taskTimeoutSeconds,
                               Runnable runner) {
        DistributedLock lock = new RedisLock(redisTemplate, taskName);
        boolean success = lock.lock(taskTimeoutSeconds, TimeUnit.SECONDS);
        if (success) { //获得了锁
            try {
                logger.debug("taskName:{} is successed to get lock, prepare to run..", taskName);
                runner.run();
            } finally {
                lock.releaseLock();
            }
        } else {
            logger.debug("taskName:{} failed to get lock!", taskName);
        }

    }

    /**
     * 执行互斥任务
     * @param taskName 任务名称
     * @param runner 任务实现
     * @param redisTemplate redis视力
     */
    public static void execute(CuratorFramework zkClient, String taskName, Runnable runner) {
        DistributedLock lock = new ZKLock(taskName, zkClient);
        boolean success = lock.lock(0, TimeUnit.SECONDS);
        if (success) { //获得了锁
            try {
                logger.debug("taskName:{} is successed to get lock, prepare to run..", taskName);
                runner.run();
            } finally {
                lock.releaseLock();
            }
        } else {
            logger.debug("taskName:{} failed to get lock!", taskName);
        }
    }

}
