package com.github.pigeon.api.utils.executors;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 互斥任务执行
 * 利用分布式锁，保证统一个任务只能有一个任务被执行
 * @author liuhaoyong on 2016/12/13.
 */
public class MutexTaskExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MutexTaskExecutor.class);

    private String taskName;

    private StringRedisTemplate enhancedCodisClient;

    private MutexTaskRunner runner;

    private int time = 10 * 60; //默认十分钟

    /**
     * @param taskName
     * @param enhancedCodisClient
     */
    private MutexTaskExecutor(String taskName, StringRedisTemplate enhancedCodisClient,MutexTaskRunner runner) {
        Objects.requireNonNull(taskName);
        Objects.requireNonNull(enhancedCodisClient);
        Objects.requireNonNull(runner);
        this.taskName = taskName;
        this.enhancedCodisClient = enhancedCodisClient;
        this.runner = runner;
    }

    private MutexTaskExecutor(String taskName, StringRedisTemplate enhancedCodisClient,MutexTaskRunner runner,int time) {
        Objects.requireNonNull(taskName);
        Objects.requireNonNull(enhancedCodisClient);
        Objects.requireNonNull(runner);
        this.taskName = taskName;
        this.enhancedCodisClient = enhancedCodisClient;
        this.runner = runner;
        this.time = time <=0? 10 * 60 : time;
    }

    /**
     *
     * @param taskName
     * @param enhancedCodisClient
     * @return
     */
    public static MutexTaskExecutor newMutexTaskExecutor(String taskName, StringRedisTemplate enhancedCodisClient,MutexTaskRunner runner){
        return new MutexTaskExecutor(taskName, enhancedCodisClient,runner);
    }


    /**
     * 执行
     * @param asyn 异步 ：true  同步：false
     */
    public void start(boolean asyn){
        final CLock lock = new CacheLock(enhancedCodisClient,taskName);
        try {
            if(lock.tryLock(time,TimeUnit.SECONDS)){ //获得了锁
                logger.info("taskName:{} is successed to get lock, prepare to run..",taskName);
                if (asyn){ //异步执行任务
                    Thread thread = new Thread(()->{
                        doJob(lock,runner);
                    },"MutexTaskExecutor-"+taskName);
                    thread.start();
                }else{//同步执行任务
                    doJob(lock,runner);
                }
            }else{
                logger.info("taskName:{} failed to get lock!",taskName);
            }
        } catch (InterruptedException e) {
            logger.error("任务启动失败", e);
        }
    }

    /**
     *
     * @param lock
     * @param runner
     */
    private void doJob(final CLock lock,MutexTaskRunner runner){
        try{
            runner.run();
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }finally {
            lock.unlock();
            logger.info("taskName:{}'s lock is free...", taskName);
        }
    }
}
