package com.cas.controller;

import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RScript;
import org.redisson.api.RSemaphore;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;

/**
 * @author xiang_long
 * @version 1.0
 * @date 2022/12/7 11:09 上午
 * @desc
 */
@RestController
public class RedissonController {

    @Resource
    private RedissonClient redissonClient;

    private Integer index = 1;

    /**
     * 滑动窗口限流
     *
     * @throws Exception
     */
    @GetMapping("limit")
    public void limit() throws Exception {
        Thread.sleep(300L);
        RRateLimiter newRateLimiter = redissonClient.getRateLimiter("20221108100128292024");
        newRateLimiter.trySetRate(RateType.OVERALL, Integer.parseInt("150"), 1, RateIntervalUnit.SECONDS);
        System.out.println(index++);
        if (!newRateLimiter.tryAcquire(1)) {
            index = 0;
            System.out.println(("每秒调用超过限流==="));
        }
    }

    /**
     * 分布式锁 + watch_dog
     * <p>
     * 只要占锁成功，就会启动一个定时任务【重新给锁设置过期时间，新的过期时间就是看门狗的默认时间】，每隔（【LockWatchingTimeOut看门狗默认时间】/3）这么长时间自动续期
     */
    @GetMapping("dLock")
    public void dLock() {
        //1.获取锁
        RLock myLock = redissonClient.getLock("my_lock");
        //2.手动加锁
        myLock.lock();
        try {
            //3.业务实现
            System.out.println("加锁成功~" + Thread.currentThread().getName());
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //4.手动解锁
            myLock.unlock();
            System.out.println("解锁成功~" + Thread.currentThread().getName());
        }
    }


    /**
     * lua脚本【暂时本地redis和redisson的版本不兼容，测不了】
     */
    @GetMapping("lua")
    public void lua() {
        // 定义Lua脚本内容，这里Lua脚本的任务是增加键的值并检查是否超过阈值
        String luaScript = "return redis.call('get',KEYS[1])";
        // 使用Redisson的Script模块加载Lua脚本
        RScript script = redissonClient.getScript();


        // 执行Lua脚本，KEYS和ARGV是Lua脚本中使用的参数
        // 这里假设我们对键"counter"进行操作，增加5，并检查结果是否大于10
//        Object result = script.eval(RScript.Mode.READ_ONLY, luaScript,
//                RScript.ReturnType.VALUE, Collections.singletonList("counter"));

        Object result = script.eval(RScript.Mode.READ_WRITE, "return 'Hi'",
                RScript.ReturnType.VALUE);

    }

    /**
     * 信号量Semaphore:
     * 信号量可以用来限制同时访问特定资源的线程数，当资源可用时，信号量会发放一个许可，线程获取许可后才能访问资源；
     * 访问完资源后，线程必须释放许可，以便其他线程能够继续访问。
     */
    @GetMapping("se")
    public void Semaphore() throws InterruptedException {

        // 创建一个信号量，初始许可数量为5
        RSemaphore semaphore = redissonClient.getSemaphore("mySemaphore");
        semaphore.trySetPermits(5);

        // 尝试获取一个许可，如果当前没有可用许可则阻塞等待
        semaphore.acquire();

        try {
            // 在这里执行受保护的代码
            System.out.println("Accessing the shared resource");
            Thread.sleep(4000);
        } finally {
            // 访问完成后，释放许可
            semaphore.release();
        }
    }

    /**
     * 计数信号量Semaphore:
     * CountDownLatch是一个同步辅助类，它允许一个或多个线程等待其他线程完成一系列操作后再继续执行。
     * 计数信号量初始化时有一个计数值，每次调用countDown()方法计数减一，当计数到达零时，所有等待的线程被释放。
     */
    @GetMapping("cl")
    public void CountDownLatch() throws InterruptedException {

        // 初始化一个计数信号量，初始计数值为3
        RCountDownLatch latch = redissonClient.getCountDownLatch("myLatch");
        latch.trySetCount(3);

        // 在一个线程中执行某些操作后，减少计数
        new Thread(() -> {
            // 执行操作
            System.out.println("Task completed");

            // 任务完成后，计数减一
            latch.countDown();
        }).start();

        // 其他线程或主线程等待所有任务完成
        latch.await(); // 这里会阻塞，直到计数变为0

        System.out.println("All tasks completed, continuing...");
    }

}
