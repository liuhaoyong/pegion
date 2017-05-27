package com.github.pigeon.api.utils.executors;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁
 * 
 * @author liuhaoyong on 2016/12/6.
 */
public interface DistributedLock {

    /**
     * 尝试获取分布式锁
     * @param waitTime 自旋等待的时间
     * @param lockTimeoutTime 锁释放的时间
     * @param unit 时间单位
     * @return 获取锁成功：true，获取失败 false
     */
    boolean tryLock(long waitTime, long lockTimeoutTime, TimeUnit unit) ;

    /**
     * 
     * @param leaseTime 如果锁获取成功，锁超时时间
     * @param unit 时间单位
     */
    /**
     * 获取分布式锁
     * @param lockTimeoutTime
     * @param unit
     * @return 获取锁成功：true，获取失败 false
     */
    boolean lock(long lockTimeoutTime, TimeUnit unit);

    /**
     * 是否已经锁住某个资源
     * 
     * @return 已锁：true 未锁：false
     */
    boolean isLocked();

    /**
     * 释放锁
     */
    public void releaseLock();
}
