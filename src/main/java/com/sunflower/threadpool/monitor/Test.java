package com.sunflower.threadpool.monitor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * @author songhengliang
 * @date 2020/10/9
 */
@Slf4j
public class Test {

    private static final String THREAD_POOL_NAME = "test_thread_pool";
    public static void main(String[] args) {

        MonitoringThreadPool monitoringThreadPool = new MonitoringThreadPool(THREAD_POOL_NAME,
                        10, 20,
                        0, TimeUnit.SECONDS,
                        new LinkedBlockingDeque<>(),
                        new ThreadFactoryBuilder().setNameFormat(THREAD_POOL_NAME + "-%d").setDaemon(true).build(),
                        new AbortPolicy());

        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            monitoringThreadPool.execute(new TestTask(i));

            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private static class TestTask implements Runnable {
        private int i;

        public TestTask(int i) {
            this.i = i;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("task execute {}", i);
            System.out.println("task execute " + i);
        }
    }

}
