package com.github.pigeon.test;
import org.apache.curator.framework.CuratorFramework;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.github.pigeon.api.utils.executors.MutexTaskExecutor;

public class MutexTaskTest  extends BaseTest{

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    private CuratorFramework         zkClient;
    
    @Test
    public void testMutexTask()
    {
        
//        RedisLock lock = new RedisLock(stringRedisTemplate,"test343");
//        System.out.println(lock.lock(1, TimeUnit.MINUTES));
//        System.out.println(lock.lock(1, TimeUnit.MINUTES));
//        lock.releaseLock();
//        System.out.println(lock.lock(1, TimeUnit.MINUTES));
//        lock.releaseLock();
        
        MutexTaskExecutor.execute(zkClient, "test1", new Runnable(){

            @Override
            public void run() {
                System.out.print("test1开始执行");
                try {
                    Thread.currentThread().sleep(1000*100);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                System.out.print("test1执行结束");
            }
        });
        
        MutexTaskExecutor.execute(zkClient, "test1", new Runnable(){

            @Override
            public void run() {
                System.out.print("test2开始执行");
                try {
                    Thread.currentThread().sleep(1000*100);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                System.out.print("test2执行结束");
            }
        });
        
    }
    
}
