package com.github.pigeon.api.utils.executors;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;
import org.springframework.util.Assert;

/**
 * 增加task执行前清除MDC的逻辑
 * 其余逻辑跟spring ThreadPoolTaskExecutor一致
 *
 * @author liuhaoyong
 *         time : 2015年11月4日 下午5:22:23
 */
public class MDCThreadPoolExecutor extends ExecutorConfigurationSupport implements SchedulingTaskExecutor {

    private static final long serialVersionUID = -5346669667792162581L;

    private final Object poolSizeMonitor = new Object();

    private int corePoolSize = 5;

    private int maxPoolSize = 10;

    private int keepAliveSeconds = 60;

    private boolean allowCoreThreadTimeOut = false;

    private int queueCapacity = 10000;

    private String pname = "defaultName";

    private ThreadPoolExecutor threadPoolExecutor;
    private static final Logger logger = LoggerFactory.getLogger(MDCThreadPoolExecutor.class);


    /**
     * Set the ThreadPoolExecutor's core pool size.
     * Default is 1.
     * <p><b>This setting can be modified at runtime, for example through JMX.</b>
     */
    public void setCorePoolSize(int corePoolSize) {
        synchronized (this.poolSizeMonitor) {
            this.corePoolSize = corePoolSize;
            if (this.threadPoolExecutor != null) {
                this.threadPoolExecutor.setCorePoolSize(corePoolSize);
            }
        }
    }

    /**
     * Return the ThreadPoolExecutor's core pool size.
     */
    public int getCorePoolSize() {
        synchronized (this.poolSizeMonitor) {
            return this.corePoolSize;
        }
    }

    /**
     * Set the ThreadPoolExecutor's maximum pool size.
     * Default is <code>Integer.MAX_VALUE</code>.
     * <p><b>This setting can be modified at runtime, for example through JMX.</b>
     */
    public void setMaxPoolSize(int maxPoolSize) {
        synchronized (this.poolSizeMonitor) {
            this.maxPoolSize = maxPoolSize;
            if (this.threadPoolExecutor != null) {
                this.threadPoolExecutor.setMaximumPoolSize(maxPoolSize);
            }
        }
    }

    /**
     * Return the ThreadPoolExecutor's maximum pool size.
     */
    public int getMaxPoolSize() {
        synchronized (this.poolSizeMonitor) {
            return this.maxPoolSize;
        }
    }

    /**
     * Set the ThreadPoolExecutor's keep-alive seconds.
     * Default is 60.
     * <p><b>This setting can be modified at runtime, for example through JMX.</b>
     */
    public void setKeepAliveSeconds(int keepAliveSeconds) {
        synchronized (this.poolSizeMonitor) {
            this.keepAliveSeconds = keepAliveSeconds;
            if (this.threadPoolExecutor != null) {
                this.threadPoolExecutor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Return the ThreadPoolExecutor's keep-alive seconds.
     */
    public int getKeepAliveSeconds() {
        synchronized (this.poolSizeMonitor) {
            return this.keepAliveSeconds;
        }
    }

    /**
     * Specify whether to allow core threads to time out. This enables dynamic
     * growing and shrinking even in combination with a non-zero queue (since
     * the max pool size will only grow once the queue is full).
     * <p>Default is "false". Note that this feature is only available on Java 6
     * or above. On Java 5, consider switching to the backport-concurrent
     * version of ThreadPoolTaskExecutor which also supports this feature.
     *
     * @see ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
     */
    public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
    }

    /**
     * Set the capacity for the ThreadPoolExecutor's BlockingQueue.
     * Default is <code>Integer.MAX_VALUE</code>.
     * <p>Any positive value will lead to a LinkedBlockingQueue instance;
     * any other value will lead to a SynchronousQueue instance.
     *
     * @see LinkedBlockingQueue
     * @see SynchronousQueue
     */
    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    @Override
    protected ExecutorService initializeExecutor(
            ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

        //设置等待任务执行完成后再关闭容器
        super.setWaitForTasksToCompleteOnShutdown(true);

        BlockingQueue<Runnable> queue = createQueue(this.queueCapacity);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                this.corePoolSize, this.maxPoolSize, this.keepAliveSeconds, TimeUnit.SECONDS,
                queue, threadFactory, rejectedExecutionHandler) {
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);

                //执行任务前清楚MDC上下文，以解决MDC线程上下问继承父线程上下文的问题
                MDC.clear();
            }
        };
        if (this.allowCoreThreadTimeOut) {
            executor.allowCoreThreadTimeOut(true);
        }

        this.threadPoolExecutor = executor;
        return executor;
    }

    /**
     * Create the BlockingQueue to use for the ThreadPoolExecutor.
     * <p>A LinkedBlockingQueue instance will be created for a positive
     * capacity value; a SynchronousQueue else.
     *
     * @param queueCapacity the specified queue capacity
     * @return the BlockingQueue instance
     * @see LinkedBlockingQueue
     * @see SynchronousQueue
     */
    protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
        if (queueCapacity > 0) {
            return new LinkedBlockingQueue<Runnable>(queueCapacity);
        } else {
            return new SynchronousQueue<Runnable>();
        }
    }

    /**
     * Return the underlying ThreadPoolExecutor for native access.
     *
     * @return the underlying ThreadPoolExecutor (never <code>null</code>)
     * @throws IllegalStateException if the ThreadPoolTaskExecutor hasn't been initialized yet
     */
    public ThreadPoolExecutor getThreadPoolExecutor() throws IllegalStateException {
        Assert.state(this.threadPoolExecutor != null, "ThreadPoolTaskExecutor not initialized");
        return this.threadPoolExecutor;
    }

    /**
     * Return the current pool size.
     *
     * @see ThreadPoolExecutor#getPoolSize()
     */
    public int getPoolSize() {
        return getThreadPoolExecutor().getPoolSize();
    }

    /**
     * Return the number of currently active threads.
     *
     * @see ThreadPoolExecutor#getActiveCount()
     */
    public int getActiveCount() {
        return getThreadPoolExecutor().getActiveCount();
    }

    @Override
    public void execute(Runnable task) {
        Executor executor = getThreadPoolExecutor();
        try {
            executor.execute(task);
        } catch (RejectedExecutionException ex) {
            logger.warn("[PigeonEvent][PoolReject]->{}", ex.getMessage());
            throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
        }
    }


    @Override
    public void execute(Runnable task, long startTimeout) {
        execute(task);
    }

    @Override
    public Future<?> submit(Runnable task) {
        ExecutorService executor = getThreadPoolExecutor();
        try {
            return executor.submit(task);
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        ExecutorService executor = getThreadPoolExecutor();
        try {
            return executor.submit(task);
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
        }
    }

    /**
     * This task executor prefers short-lived work units.
     */
    @Override
    public boolean prefersShortLivedTasks() {
        return true;
    }

    public void setPname(String pname) {
        this.pname = pname;
    }


}
