package com.github.pigeon.api.utils.executors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * jdk CountDownLatch 封装
 * @author liuhaoyong on 2016/12/13.
 */
public class CountDownExecutor {
    private static final Logger logger = LoggerFactory.getLogger(CountDownExecutor.class);

    private CountDownLatch end = null;
    private ExecutorService exec = null;
    private List<CountDownRunner> list = null;

    private CountDownExecutor(CountDownLatch end,ExecutorService exec,List<CountDownRunner> list){
        this.end = end;
        this.exec = exec;
        this.list = list;
    }

    /**
     *
     * @param count countDown数
     * @param name 名称
     * @return
     */
    public static CountDownExecutor newCountDown(int count,String name){
        return new CountDownExecutor(new CountDownLatch(count),
                Executors.newFixedThreadPool(count),
                new ArrayList<>(count));
    }

    /**
     *
     * @param countDownRunner
     * @return
     */
    public CountDownExecutor addRunner(CountDownRunner countDownRunner){
        list.add(countDownRunner);
        return this;
    }

    /**
     *
     */
    public void start(){
        list.stream().forEach(r -> {
            exec.execute(()->{
                try{
                    r.run();
                }catch (Exception e){
                    logger.error(e.getMessage(),e);
                }finally {
                    end.countDown();
                    logger.info(Thread.currentThread().getName()+"-->countDownNum:{}",end.getCount());
                }
            });
        });
        try {
            end.await();
            if(exec != null){
                exec.shutdown();
                logger.info("exec is shutdown........");
            }
        }catch (InterruptedException e){
            logger.error(e.getMessage(),e);
            Thread.currentThread().interrupt();
        }
    }


}
