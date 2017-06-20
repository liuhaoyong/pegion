package com.github.pigeon.api.utils.executors;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于zk的分布式锁
 * 
 * @author liuhaoyong 2017年6月20日 下午3:31:58
 */
public class ZKLock implements DistributedLock {
    
    private static final Logger logger = LoggerFactory.getLogger(ZKLock.class); 
    
    private static final String BASE_PATH="/finance/lock/";
    
    private InterProcessLock lock;
    
    public ZKLock(String lockName, CuratorFramework frameword)
    {
        lock = new InterProcessSemaphoreMutex(frameword, BASE_PATH+lockName);
    }
    
    @Override
    public boolean tryLock(long waitTime, long lockTimeoutTime, TimeUnit unit) {
        try {
            return lock.acquire(waitTime,unit);
        } catch (Exception e) {
            logger.info("获取zk锁异常", e);
            return false;
        }
    }

    @Override
    public boolean lock(long lockTimeoutTime, TimeUnit unit) {
        try {
            return lock.acquire(0,TimeUnit.MICROSECONDS);
        } catch (Exception e) {
            logger.info("获取zk锁异常", e);
            return false;
        }
    }

    @Override
    public boolean isLocked() {
        return lock.isAcquiredInThisProcess();
    }

    @Override
    public void releaseLock() {
        try {
            lock.release();
        } catch (Exception e) {
            logger.info("释放zk锁异常", e);
        }
    }
    
}
