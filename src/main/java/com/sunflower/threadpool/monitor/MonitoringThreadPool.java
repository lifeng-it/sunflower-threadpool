package com.sunflower.threadpool.monitor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sunflower.threadpool.utils.IpUtils;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

/**
 * @author songhengliang
 * @date 2020/10/14
 */
@Slf4j
public class MonitoringThreadPool extends ThreadPoolExecutor {
    /**
     * 最近任务到达时间：execute方法中设置
     * 1    execute方法中设置
     */
    private volatile long latestArrivalTime;

    /**
     * 任务的服务时间总和，包括等待时间
     */
    private AtomicLong totalServiceTime = new AtomicLong();

    /**
     * 处理完的任务数
     * 1    after中加1？
     */
    private AtomicLong numbersOfTasksRetired = new AtomicLong();

    /**
     * 即将被执行的任务数：
     * 1    在execute中加1 在after中减1
     */
    private AtomicLong numbersOfTasksBeforeReallyExecute = new AtomicLong();

    /**
     * 在前一秒时，即将被执行的任务数
     *  即：当前时间向前推1秒时的 "即将被执行的任务数"
     * 1
     */
    private AtomicLong last1SecondTaskNum = new AtomicLong();

    /**
     * 正在被执行的任务数
     * 1
     */
    private AtomicInteger numberOfTasksExecuting = new AtomicInteger();







    /**
     * 任务在池中的等待时间
     * 1    after - 最初记录的时间？
     */
    private AtomicLong totalPoolTime = new AtomicLong();

    /**
     * 任务到达间隔之和
     */
    private AtomicLong aggregateInterTaskArrivalTime = new AtomicLong();





    /**
     * 1秒钟执行的任务数  todo
     * 1    当前"即将被执行的任务数" - 前一秒"即将被执行的任务数"
     */



    private ScheduledExecutorService scheduledExecutorService;
    private String systemCode;

    /**
     * 线程池名称
     */
    private String name;
    private boolean setLogUnique;

    public MonitoringThreadPool(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime,
        TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
            RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory,
                handler);

        this.name = name;
        this.setLogUnique = true;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor
                ((new ThreadFactoryBuilder()).setDaemon(true).build());

        this.scheduledExecutorService.scheduleWithFixedDelay(
                () -> {
                    //打点到influxdb

                    /*
                    指标：
                        ip：
                        systemCode：系统code
                        name：线程池名称
                        poolSize：线程池当前的线程数量
                        activeThreadNum：当前线程池中正在执行任务的线程数量
                        queueSize：队列长度

                        totalTaskNum：线程池已经执行的和未执行的任务总数
                        completedTaskNum：线程池已完成的任务数量，该值小于等于totalTaskNum
                        largestPoolSize：线程池曾经创建过的最大线程数量。通过这个数据可以知道线程池是否满过，也就是达到了maximumPoolSize


                        numberOfTasksExecuting：正在被执行的任务数
                        numbersOfTasksBeforeReallyExecute：即将被执行的任务数
                     */

                    String ip = IpUtils.getLocalHostAddress();
                    int poolSize = this.getPoolSize();
                    int activeThreadNum = this.getActiveCount();
                    int queueSize = this.getQueue().size();
                    long totalTaskNum = this.getTaskCount();
                    long completedTaskNum = this.getCompletedTaskCount();
                    int largestPoolSize = this.getLargestPoolSize();
                    StringBuilder pointMsg = new StringBuilder();
                    pointMsg.append("ip:").append(ip).append(", ")
                            .append("systemCode:").append("sunflower-threadpool").append(", ")
                            .append("name:").append(name).append(", ")
                            .append("poolSize:").append(poolSize).append(", ")
                            .append("activeThreadNum:").append(activeThreadNum).append(", ")
                            .append("queueSize:").append(queueSize).append(", ")
                            .append("totalTaskNum:").append(totalTaskNum).append(", ")
                            .append("completedTaskNum:").append(completedTaskNum).append(", ")
                            .append("largestPoolSize:").append(largestPoolSize).append(", ")
                            .append("numberOfTasksExecuting:").append(numberOfTasksExecuting.get()).append(", ")
                            .append("numbersOfTasksBeforeReallyExecute:").append(numbersOfTasksBeforeReallyExecute.get()).append(", ");
//                    log.info(pointMsg.toString());
                    System.out.println(pointMsg.toString());
                },
                0, 1, TimeUnit.SECONDS);
    }

    public MonitoringThreadPool(String name, int corePoolSize, int maximumPoolSize,
            long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        this(name, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                new ThreadFactoryBuilder().setNameFormat(name + "-%d").setDaemon(true).build(),
                new AbortPolicy());
    }

    @Override
    public void execute(Runnable command) {
        long now = System.currentTimeMillis();
        MonitoringTask task = MonitoringTask.builder().submitTime(now).task(command).build();

        try {
            this.latestArrivalTime = now;

            //即将被执行的任务数 +1
            this.numbersOfTasksBeforeReallyExecute.incrementAndGet();
        } finally {
            super.execute(task);
        }
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {

        MonitoringTask monitoringTask = (MonitoringTask) r;

        //任务在线程池中的等待时间
        this.totalPoolTime.addAndGet(System.currentTimeMillis() - monitoringTask.getSubmitTime());

        //正在被执行的任务数 +1
        int i = this.numberOfTasksExecuting.incrementAndGet();
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        MonitoringTask monitoringTask = (MonitoringTask) r;

        //即将被执行的任务数 -1
        this.numbersOfTasksBeforeReallyExecute.decrementAndGet();

        //正在被执行的任务数 -1
        this.numberOfTasksExecuting.decrementAndGet();

        //任务的服务时间总和，包括等待时间
        this.totalServiceTime.addAndGet(System.currentTimeMillis() - monitoringTask.getSubmitTime());

        //处理完的任务数
        this.numbersOfTasksRetired.incrementAndGet();
    }

    @Override
    public void shutdown() {
        log.info("threadpool:{} shutdown", name);
        super.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        log.info("threadpool:{} shutdown now", name);
        return super.shutdownNow();
    }
}
