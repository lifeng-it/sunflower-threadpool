package com.sunflower.threadpool.monitor;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author songhengliang
 * @date 2020/10/14
 */
@Getter
@Setter
@Builder
public class MonitoringTask implements Runnable {
    /**
     * 提交任务的时间: ms
     */
    private long submitTime;

    /**
     * 开始执行任务的时间
     */
    private long startExecuteTime;

    /**
     * 日志唯一key
     */
    private String logUniqueKey;

    /**
     * 要执行的任务
     */
    private Runnable task;

    @Override
    public void run() {
        this.task.run();
    }
}
