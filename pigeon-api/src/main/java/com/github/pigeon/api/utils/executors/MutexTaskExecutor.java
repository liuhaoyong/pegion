package com.github.pigeon.api.utils.executors;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 互斥任务执行 利用分布式锁，保证同一个任务只能有一个任务被执行
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
        final DistributedLock lock = new RedisLock(redisTemplate, taskName);
        if (lock.lock(taskTimeoutSeconds, TimeUnit.SECONDS)) { //获得了锁
            logger.info("taskName:{} is successed to get lock, prepare to run..", taskName);
            if (asyn) { //异步执行任务
                Thread thread = new Thread(() -> {
                    doJob(lock, runner, taskName);
                }, "MutexTaskExecutor-" + taskName);
                thread.start();
            } else {//同步执行任务
                doJob(lock, runner, taskName);
            }
        } else {
            logger.info("taskName:{} failed to get lock!", taskName);
        }

    }

    /**
     * @param lock
     * @param runner
     */
    private static void doJob(final DistributedLock lock, Runnable runner, String taskName) {
        try {
            runner.run();
        } finally {
            lock.releaseLock();
            logger.info("taskName:{}'s lock is free...", taskName);
        }
    }
}
