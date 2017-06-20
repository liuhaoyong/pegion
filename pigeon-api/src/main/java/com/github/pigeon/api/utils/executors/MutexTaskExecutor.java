package com.github.pigeon.api.utils.executors;

import java.util.concurrent.TimeUnit;

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
     * @param taskTimeoutSeconds 任务的最大执行时间
     * @param taskName  任务名称
     * @param redisTemplate redis视力
     * @param asyn 是否异步
     * @param runner 任务实现
     */
    public static void execute(long taskTimeoutSeconds, String taskName,
                              StringRedisTemplate redisTemplate, boolean asyn, Runnable runner) {
        DistributedLock lock = new RedisLock(redisTemplate, taskName);
        boolean success = lock.lock(taskTimeoutSeconds, TimeUnit.SECONDS);
        if (success) { //获得了锁
            logger.info("taskName:{} is successed to get lock, prepare to run..", taskName);
            if (asyn) { //异步执行任务
                Thread thread = new Thread(() -> {
                        runner.run();
                }, "MutexTaskExecutor-" + taskName);
                thread.start();
            } else {//同步执行任务
                    runner.run();
            }
        } else {
            logger.info("taskName:{} failed to get lock!", taskName);
        }

    }
}
