package com.xu.thread;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entry.MybatiusPlusTest;
import com.example.demo.mapper.MybatisPlusTestMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskTest {

    @Autowired
    private MybatisPlusTestMapper mapper;


    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(10,// 核心线程池大小
            20,// 最大线程池大小
            10,// 线程最大空闲时间
            TimeUnit.MILLISECONDS,// 时间单位
            new ArrayBlockingQueue<>(1000),// 线程等待队列
            Executors.defaultThreadFactory(),// 线程创建工厂
            new ThreadPoolExecutor.AbortPolicy());// 拒绝策略

    private final AtomicInteger atomic = new AtomicInteger();

    public static void main(String[] args) throws Exception {
        TaskTest test = new TaskTest();
        List<MybatiusPlusTest> allResult = test.run().get();
        //TODO allResult进行 修改
        executor.shutdown();
    }

    public CompletableFuture<List<MybatiusPlusTest>> run() {

        List<CompletableFuture<List<MybatiusPlusTest>>> future = new ArrayList<>();

        for (int i = 1; i < 11; i++) {
            future.add(task(i));
        }

        return CompletableFuture.supplyAsync(() -> {
            List<MybatiusPlusTest> list = new ArrayList<>();
            future.stream().parallel().forEach(task1 -> {
                list.addAll(task1.join());
            });
            return list;
        });
    }

    public CompletableFuture<List<MybatiusPlusTest>> task(Integer i) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO:任务
            Page<MybatiusPlusTest> mybatiusPlusTestPage = mapper.selectPage(new Page<>(i, 2), new QueryWrapper<>());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return new ArrayList<MybatiusPlusTest>() {{
                addAll(mybatiusPlusTestPage.getRecords());
            }};
        }, executor);
    }

}