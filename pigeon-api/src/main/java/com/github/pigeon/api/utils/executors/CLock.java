package com.github.pigeon.api.utils.executors;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * 分布式锁,实现了Java的Lock接口
 * @author liuhaoyong on 2016/12/6.
 */
public interface CLock extends Lock {

    /**
     * 尝试获取分布式锁
     * @param waitTime 自旋等待的时间
     * @param leaseTime 锁释放的时间
     * @param unit 时间单位
     * @return 获取锁成功：True，获取失败 false
     * @throws InterruptedException
     */
    boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;

    /**
     *  尝试获取锁，如果获取失败会抛出InterruptedException
     * @param leaseTime 锁释放时间
     * @param unit 时间单位
     * @throws InterruptedException
     */
    void lockInterruptibly(long leaseTime, TimeUnit unit) throws InterruptedException;

    /**
     * 获取分布式锁，如果获取锁失败会尝试中断当前线程
     * @param leaseTime 锁释放的时间
     * @param unit 时间单位
     */
    void lock(long leaseTime, TimeUnit unit);


    /**
     * 查询是否已经锁住某个资源
     * @return 已锁：true 未锁：false
     */
    boolean isLocked();
}
