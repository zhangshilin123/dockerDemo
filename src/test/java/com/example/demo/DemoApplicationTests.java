package com.example.demo;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entry.MybatiusPlusTest;
import com.example.demo.mapper.MybatisPlusTestMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SpringBootTest
@Slf4j
@EnableTransactionManagement
public class DemoApplicationTests {

    @Autowired
    private MybatisPlusTestMapper mapper;

    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    AtomicInteger atomicInteger = new AtomicInteger();

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(10,// 核心线程池大小
            20,// 最大线程池大小
            10,// 线程最大空闲时间
            TimeUnit.MILLISECONDS,// 时间单位
            new ArrayBlockingQueue<>(1000),// 线程等待队列
            Executors.defaultThreadFactory(),// 线程创建工厂
            new ThreadPoolExecutor.AbortPolicy());// 拒绝策略


    @Test
    public void test() throws ExecutionException, InterruptedException {
         List<MybatiusPlusTest> allResult = run().get();
        log.error("全部的数据：{}", allResult);
        log.error("总共的执行线程个数为：{}",atomicInteger.get());
        //TODO 或者一整个批次进行修改
        executor.shutdown();
    }

    public CompletableFuture<List<MybatiusPlusTest>> run() {
        List<CompletableFuture<List<MybatiusPlusTest>>> future = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
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
            atomicInteger.addAndGet(1);
            // TODO:任务
            List<MybatiusPlusTest> records;
            try {
                // 读锁
                readLock.lock();
                log.error(Thread.currentThread().getName()+"线程个数是");
                Page<MybatiusPlusTest> mybatiusPlusTestPage = mapper.selectPage(new Page<>(i, 2), new QueryWrapper<>());
                records = mybatiusPlusTestPage.getRecords();
            } finally {
                readLock.unlock();
            }
            try {
                writeLock.lock();
                records.forEach(t -> t.setMybatisPlus("6666"));
                records.forEach(mapper::updateById);
            } finally {
                writeLock.unlock();
            }
            return new ArrayList<MybatiusPlusTest>() {{
                addAll(records);
            }};
        }, executor);
    }
//
//
//    @Test
//    public void futureTest() {
//        CompletableFuture<List<String>> ids;
//
//        //thenComposeAsync 将流当作入参，传入，返回新的流，为空返回空
//        CompletableFuture<List<String>> result = ids.thenComposeAsync(l -> {
//            Stream<CompletableFuture<String>> zip =
//                    l.stream().map(i -> {
//                        CompletableFuture<String> nameTask = ifhName(i);
//                        CompletableFuture<Integer> statTask = ifhStat(i);
//
//                        //thenCombineAsync，合并两个流，并将结果当作入参传入
//                        return nameTask.thenCombineAsync(statTask, (name, stat) -> "Name " + name + " has stats " + stat);
//                    });
//            List<CompletableFuture<String>> combinationList = zip.collect(Collectors.toList());
//            CompletableFuture<String>[] combinationArray = combinationList.toArray(new CompletableFuture[combinationList.size()]);
//
//            CompletableFuture<Void> allDone = CompletableFuture.allOf(combinationArray);
//
//
//            return allDone.thenApply(v -> combinationList.stream()
//                    .map(CompletableFuture::join)
//                    .collect(Collectors.toList()));
//
//        });
//
//        List<String> results = result.join();
//        assertThat(results).contains(
//                "Name NameJoe has stats 103",
//                "Name NameBart has stats 104",
//                "Name NameHenry has stats 105",
//                "Name NameNicole has stats 106",
//                "Name NameABSLAJNFOAJNFOANFANSF has stats 121");
//    }
//
//    @Test
//    public void testReact() {
//        Flux<String> ids = ifhrIds();
//        Flux<String> combinations = ids.flatMap(id -> {
//            Mono<String> nameTask = ifhrName(id);
//            Mono<Integer> statTask = ifhrStat(id);
//
//            return nameTask.zipWith(statTask, (name, stat) ->
//                    "Name " + name + " has stats " + stat);
//        });
//
//        // 一个 Flux 对象代表一个包含 0..N 个元素的响应式序列，而一个 Mono 对象代表一个包含 零/一个（0..1）元素的结果
//        Mono<List<String>> listMono = combinations.collectList();
//        List<String> results = listMono.block();
//
//        assertThat(results).containsExactly(
//                "Name NameJoe has stats 103",
//                "Name NameBart has stats 104",
//                "Name NameHenry has stats 105",
//                "Name NameNicole has stats 106",
//                "Name NameABSLAJNFOAJNFOANFANSF has stats 121"
//        );
//    }
//
//    public void testCreate() {
//
//        //flatmap c合并流为一个输出
//        List<String> stringList = Arrays.asList("hello", "world");
//        List<String> collect = stringList.stream()
//                .map(str -> str.split(""))
//                .flatMap(Arrays::stream)
//                .distinct()
//                .collect(Collectors.toList());
//
//        //所有的树结构是：[{"id":1,"name":"河北","parent":0,"children":[{"id":2,"name":"邯郸","parent":1,"children":[{"id":3,"name":"成安","parent":2,"children":[]}]}]},{"id":4,"name":"北京","parent":0,"children":[{"id":5,"name":"北京市","parent":4,"children":[{"id":6,"name":"朝阳区","parent":5,"children":[]}]}]}]
//        List<TreeVO> treeVOS = Lists.newArrayList();
//        List<TreeVO> treeVOS1 = treeVOS.stream().flatMap(list -> list.getChildrenies().stream()).collect(Collectors.toList());
//
//        List<TreeVO> treeVOS2 = treeVOS.stream().flatMap(list ->
//                list.getChildrenies().stream()
//                        .flatMap(t -> t.getChildrenies().stream())
//        ).collect(Collectors.toList());
//
//        //List<list<T>>类型的结构
//        List<List<TreeVO>> lists = Lists.newArrayList();
//        List<String> collect2 = lists.stream().flatMap(Collection::stream).map(t -> t.getName()).collect(Collectors.toList());
//
//
//        Flux<String> seq1 = Flux.just("foo", "bar", "foobar");
//
//        List<String> iterable = Arrays.asList("foo", "bar", "foobar");
//        List<Object> collect1 = iterable.stream().map(t -> t.split(","))
//                .flatMap(Arrays::stream).collect(Collectors.toList());
//        Flux<String> seq2 = Flux.fromIterable(iterable);
//
//        Mono<String> empty = Mono.empty();
//        Mono<String> just = Mono.just("aaa");
//
//        Flux.range(5, 3); //5,6,7
//
//        /**
//         *
//         * 5种订阅模式
//         *
//         * subscribe();
//         *
//         * subscribe(Consumer<? super T> consumer);
//         *
//         * subscribe(Consumer<? super T> consumer,
//         *           Consumer<? super Throwable> errorConsumer);
//         *
//         * subscribe(Consumer<? super T> consumer,
//         *           Consumer<? super Throwable> errorConsumer,
//         *           Runnable completeConsumer);
//         *
//         * subscribe(Consumer<? super T> consumer,
//         *           Consumer<? super Throwable> errorConsumer,
//         *           Runnable completeConsumer,
//         *           Consumer<? super Subscription> subscriptionConsumer);
//         */
//
//        //1. 最简单的订阅  配置一个在订阅时会产生3个值的 Flux
//        Flux<Integer> ints = Flux.range(1, 3);
//        ints.subscribe();
//        Flux<Integer> ints1 = Flux.range(1, 3);
//        ints.subscribe(i -> System.out.println(i));
//
//        //2. 处理 接入的值以及错误的处理
//        Flux<Integer> ints2 = Flux.range(1, 4)
//                .map(i -> {
//                    if (i <= 3) return i;
//                    throw new RuntimeException("Got to 4");
//                });
//        ints.subscribe(i -> System.out.println(i),
//                error -> System.err.println("Error: " + error));
//
//        //3.处理 接入的值以及错误的处理  以及完成之后的处理
//        // 处理完成信号的 lambda 是一对空的括号，因为它实际上匹配的是 Runnalbe 接口中的 run 方法
//        Flux<Integer> ints3 = Flux.range(1, 4);
//        ints.subscribe(i -> System.out.println(i),
//                error -> System.err.println("Error " + error),
//                () -> System.out.println("Done"));
//
//        //4.还支持一个新的自定义的 subscriber
//        SampleSubscriber<Integer> ss = new SampleSubscriber<Integer>();
//        Flux<Integer> ints4 = Flux.range(1, 4);
//        ints.subscribe(i -> System.out.println(i),
//                error -> System.err.println("Error " + error),
//                () -> {
//                    System.out.println("Done");
//                },
//                s -> ss.request(10));
//        ints.subscribe(ss);
//
//        //5.另一个 subscribe 方法的签名，它只接收一个自定义的 Subscriber， 没有其他的参数，如下所示：
//        //subscribe(Subscriber<? super T> subscriber);
//        //如果你已经有一个 Subscriber，那么这个方法签名还是挺有用的。
//        // 况且，你可能还会用到它 来做一些订阅相关（subscription-related）的回调。比如，你想要自定义“背压（backpressure）” 并且自己来触发请求
//        //request(n) 就是这样一个方法。它能够在任何 hook 中，通过 subscription 向上游传递 背压请求。这里我们在开始这个流的时候请求1个元素值。
//        //BaseSubscriber 还提供了 requestUnbounded() 方法来切换到“无限”模式（等同于 request(Long.MAX_VALUE)）。
//        Flux<String> source = Flux.just("dd", "dfs");
//        source.subscribe(new BaseSubscriber<String>() {
//            @Override
//            protected void hookOnSubscribe(Subscription subscription) {
//
//                request(1);
//            }
//
//            @Override
//            protected void hookOnNext(String value) {
//                request(1);
//            }
//        });
//
//
//        Flux<String> flux = Flux.generate(
//                () -> 0,
//                //BiFunction 多元入参，一个返回值的函数接口  BiFunction<T, U, R>
//                (state, sink) -> {
//                    //根据状态值生成下一个数
//                    sink.next("3 x " + state + " = " + 3 * state);
//                    //根据state状态值来终止流
//                    if (state == 10) sink.complete();
//                    return state + 1;
//                });
//
//        //我们也可以使用可变（mutable）类型（译者注：如上例，原生类型及其包装类，以及String等属于不可变类型） 的 <S>
//        Flux<String> flux1 = Flux.generate(
//                AtomicLong::new,
//                (state, sink) -> {
//                    long i = state.getAndIncrement();
//                    sink.next("3 x " + i + " = " + 3 * i);
//                    if (i == 10) sink.complete();
//                    return state;
//                });
//
//        Flux<String> flux2 = Flux.generate(
//                this::toString,
//                (state, sink) -> {
//                    String s = state.toUpperCase();
//                    sink.next(s + "qq");
//                    if (s.length() == 10)
//                        sink.complete();
//                    return state;
//                }
//        );
//
//        // generate 方法中增加 Consumer 的例子：
//        // state 使用了数据库连接或者其他需要最终进行清理的资源，这个 Consumer lambda 可以用来在最后关闭连接或完成相关的其他清理任务。
//        Flux<String> flux3 = Flux.generate(
//                AtomicLong::new,
//                (state, sink) -> {
//                    long i = state.getAndIncrement();
//                    sink.next("3 x " + i + " = " + 3 * i);
//                    if (i == 10) sink.complete();
//                    return state;
//                }, (state) -> System.out.println("state: " + state));
//    }
//
//
//    //作为一个更高级的创建 Flux 的方式， create 方法的生成方式既可以是同步， 也可以是异步的，并且还可以每次发出多个元素。
//    //与 generate 不同的是，create 不需要状态值，另一方面，它可以在回调中触发 多个事件（即使是在未来的某个时间）。
//    //create 有个好处就是可以将现有的 API 转为响应式，比如监听器的异步方法。
//
//    //Mono 也有一个用于 create 的生成器（generator）—— MonoSink，它不能生成多个元素， 因此会抛弃第一个元素之后的所有元素。
//
//    Flux<String> bridge = Flux.create(sink -> {
//        //4.所有这些都是在 myEventProcessor 执行时异步执行的
//        myEventProcessor.register(
//                //1. 建立一个桥接
//                new MyEventListener<String>() {
//
//                    public void onDataChunk(List<String> chunk) {
//                        //2. 每一个 chunk 的数据转化为 Flux 中的一个元素。
//                        for (String s : chunk) {
//                            sink.next(s);
//                        }
//                    }
//
//                    public void processComplete() {
//                        //3. processComplete 事件转换为 onComplete。
//                        sink.complete();
//                    }
//                });
//    });
//    /**
//     * public static enum OverflowStrategy {
//     * IGNORE,   完全忽略下游背压请求，这可能会在下游队列积满的时候导致 IllegalStateException
//     * ERROR,    当下游跟不上节奏的时候发出一个 IllegalStateException 的错误信号。
//     * DROP,     当下游没有准备好接收新的元素的时候抛弃这个元素。
//     * LATEST,   让下游只得到上游最新的元素。
//     * BUFFER;   （默认的）缓存所有下游没有来得及处理的元素（这个不限大小的缓存可能导致 OutOfMemoryError）
//     * <p>
//     * private OverflowStrategy() {
//     * }
//     * }
//     */
//
//    //push  push，适合生成事件流。与 create`类似，`push 也可以是异步地，
//    // 并且能够使用以上各种溢出策略（overflow strategies）管理背压。每次只有一个生成线程可以调用 next，complete 或 error。
//    Flux<String> bridge1 = Flux.push(sink -> {
//        myEventProcessor.register(
//                new SingleThreadEventListener<String>() {
//
//                    public void onDataChunk(List<String> chunk) {
//                        for (String s : chunk) {
//                            sink.next(s);
//                        }
//                    }
//
//                    public void processComplete() {
//                        sink.complete();
//                    }
//
//                    public void processError(Throwable e) {
//                        sink.error(e);
//                    }
//                });
//    });
//
//    //推送/拉取（push/pull）混合模式
//    //不像 push，create 可以用于 push 或 pull 模式，因此适合桥接监听器的 的 API
//    //因为事件消息会随时异步地到来。回调方法 onRequest 可以被注册到 FluxSink 以便跟踪请求
//    //这个回调可以被用于从源头请求更多数据，或者通过在下游请求到来 的时候传递数据给 sink 以实现背压管理。
//    // 这是一种推送/拉取混合的模式， 因为下游可以从上游拉取已经就绪的数据，上游也可以在数据就绪的时候将其推送到下游。
//    Flux<String> bridge = Flux.create(sink -> {
//        myMessageProcessor.register(
//                new MyMessageListener<String>() {
//                    //后续异步到达的 message 也会被发送给 sink。
//                    public void onMessage(List<String> messages) {
//                        for (String s : messages) {
//                            sink.next(s);
//                        }
//                    }
//                });
//        sink.onRequest(n -> {
//            //当有请求的时候取出一个 message。
//            List<String> messages = myMessageProcessor.request(n);
//            //如果有就绪的 message，就发送到 sink。
//            for (String s : messages) {
//                sink.next(s);
//            }
//        });
//    });
//
//    //onDispose 和 onCancel 这两个回调用于在被取消和终止后进行清理工作。
//    // onDispose 可用于在 Flux 完成，有错误出现或被取消的时候执行清理。
//    // onCancel 只用于针对“取消”信号执行相关操作，会先于 onDispose 执行。
//    Flux<String> bridge3 = Flux.create(sink -> {
//        sink.onRequest(n -> channel.poll(n))
//                .onCancel(() -> channel.cancel())
//                .onDispose(() -> channel.close())
//    });
//
//    //handle 方法有些不同，它在 Mono 和 Flux 中都有。然而，它是一个实例方法 （instance method），
//    // 意思就是它要链接在一个现有的源后使用（与其他操作符一样）
//    //handle(BiConsumer<T, SynchronousSink<R>>)
//    //我们可以使用 handle 来去掉其中的 null。
//    Flux<String> alphabet = Flux.just(-1, 30, 13, 9, 20)
//            .handle((i, sink) -> {
//                String letter = alphabet(i);
//                //就不会调用 sink.next 从而过滤掉。
//                if (letter != null)
//                    sink.next(letter);
//            });
//
//    alphabet.subscribe(System.out::println);
//
//
//    //Schedulers.newElastic(yourScheduleName) 创建一个新的名为 yourScheduleName 的弹性调度器。
//    public static void main(String[] args) {
//        //操作符基于非阻塞算法实现，从而可以利用到某些调度器的工作窃取（work stealing） 特性的好处。
//        Schedulers.newBoundedElastic(5, 10, "dynimaic", 600, false);
//
//        // 通过工厂方法 Flux.interval(Duration.ofMillis(300)) 生成的每 300ms 打点一次的 Flux<Long>，
//        // 默认情况下使用的是 Schedulers.parallel()，下边的代码演示了如何将其装换为 Schedulers.single()：
//        Flux.interval(Duration.ofMillis(300));
//        Flux.interval(Duration.ofMillis(300), Schedulers.newSingle("test"));
//        //Reactor 提供了两种在响应式链中调整调度器 Scheduler 的方法：publishOn 和 subscribeOn。
//        // 它们都接受一个 Scheduler 作为参数，从而可以改变调度器
//        //但是 publishOn 在链中出现的位置 是有讲究的，而 subscribeOn 则无所谓。要理解它们的不同
//        /**
//         * publishOn 和subscribeOn
//         *
//         * publishOn 的用法和处于订阅链（subscriber chain）中的其他操作符一样。它将上游 信号传给下游，
//         * 同时执行指定的调度器 Scheduler 的某个工作线程上的回调。 它会 改变后续的操作符的执行所在线程 （直到下一个 publishOn 出现在这个链上）。
//         *
//         * subscribeOn 用于订阅（subscription）过程，作用于那个向上的订阅链（发布者在被订阅 时才激活，订阅的传递方向是向上游的）。
//         * 所以，无论你把 subscribeOn 至于操作链的什么位置， 它都会影响到源头的线程执行环境（context）。
//         * 但是，它不会影响到后续的 publishOn，后者仍能够切换其后操作符的线程执行环境。
//         *
//         * 只有操作链中最早的 subscribeOn 调用才算数。
//         */
//
//        /**
//         *   线程模型
//         *
//         *
//         *
//         * Flux 和 Mono 不会创建线程。一些操作符，比如 publishOn，会创建线程。同时，作为一种任务共享形式，
//         * 这些操作符可能会从其他任务池（work pool）——如果其他任务池是空闲的话——那里“偷”线程。
//         * 因此， 无论是 Flux、Mono 还是 Subscriber 都应该精于线程处理。它们依赖这些操作符来管理线程和任务池。
//         *
//         *
//         * publishOn 强制下一个操作符（很可能包括下一个的下一个…​）来运行在一个不同的线程上。
//         * 类似的，subscribeOn 强制上一个操作符（很可能包括上一个的上一个…​）来运行在一个不同的线程上。 记
//         * 住，在你订阅（subscribe）前，你只是定义了处理流程，而没有启动发布者。
//         * 基于此，Reactor 可以使用这些规则来决定如何执行操作链。然后，一旦你订阅了，整个流程就开始工作了。
//         *
//         *
//         */
//
//
//        /**
//         *
//         * 内部机制保证了这些操作符能够借助自增计数器（incremental counters）和警戒条件（guard conditions） 以线程安全的方式工作。
//         * 例如，如果我们有四个线程处理一个流（就像上边的例子），每一个请求会让计数器自增， 这样后续的来自不同线程的请求就能拿到正确的元素。
//         */
//        // 创建一个有 10,000 个元素的 Flux。
//        Flux.range(1, 10000)
//                //创建等同于 CPU 个数的线程（最小为4）。
//                .publishOn(Schedulers.parallel())
//                //subscribe() 之前什么都不会发生。
//                .subscribe();
//
//        /**
//         *
//         * 处理错误
//         *
//         *
//         * 错误（error）是终止（terminal）事件。
//         * 当有错误发生时，它会导致流序列停止， 并且错误信号会沿着操作链条向下传递，直至遇到你定义的 Subscriber 及其 onError 方法。
//         *
//         *
//         * 因此，订阅者（subscriber）的 onError 方法是应该定义的。
//         * 如果没有定义，onError 会抛出 UnsupportedOperationException。
//         * 你可以接下来再 检测错误，并通过 Exceptions.isErrorCallbackNotImplemented 方法捕获和处理它。
//         *
//         *
//         * 你也许熟悉在 try-catch 代码块中处理异常的几种方法。常见的包括如下几种：
//         *
//         * 捕获并返回一个静态的缺省值。
//         * 捕获并执行一个异常处理方法。
//         * 捕获并动态计算一个候补值来顶替。
//         * 捕获，并再包装为某一个 业务相关的异常，然后再抛出业务异常。
//         * 捕获，记录错误日志，然后继续抛出。
//         * 使用 finally 来清理资源，或使用 Java 7 引入的 "try-with-resource"。
//         */
//        Flux<String> s = Flux.range(1, 10)
//                //执行 map 转换，有可能抛出异常。
//                .map(v -> doSomethingDangerous(v))
//                //如果没问题，执行第二个 map 转换操作。
//                .map(v -> doSecondTransform(v));
//        //所有转换成功的值都打印出来。
//        s.subscribe(value -> System.out.println("RECEIVED " + value),
//                //一旦有错误，序列（sequence）终止，并打印错误信息。
//                error -> System.err.println("CAUGHT " + error)
//        );
//
//        /**
//         *
//         * 静态缺省值
//         */
//        Flux.just(10)
//                .map(this::doSomethingDangerous)
//                .onErrorReturn(e -> e.getMessage().equals("boom10"), "recovered10");
//
//        /**
//         *
//         * 异常处理方法
//         */
//
//        Flux.just("key1", "key2")
//                //对于每一个 key， 异步地调用一个外部服务
//                .flatMap(k -> callExternalService(k))
//                //如果对外部服务的调用失败，则再去缓存中查找该 key。注意，这里无论 e 是什么，都会执行异常处理方法。
//                .onErrorResume(e -> getFromCache(k));
//
//        /**
//         *
//         * 动态候补值
//         */
//        //在 onErrorResume 中，使用 Mono.just 创建一个 Mono。将异常包装到另一个类中。
//        erroringFlux.onErrorResume(error -> Mono.just(
//                myWrapper.fromError(error)
//        ));
//
//        /**
//         *
//         * 捕获并重新抛出
//         */
//        Flux.just("timeout1")
//                .flatMap(k -> callExternalService(k))
//                .onErrorResume(original -> Flux.error(
//                        new BusinessException("oops, SLA exceeded", original)
//                );
//
//        //然而还有一个更加直接的方法—— onErrorMap：
//        Flux.just("timeout1")
//                .flatMap(k -> callExternalService(k))
//                .onErrorMap(original -> new BusinessException("oops, SLA exceeded", original));
//    }
//
//    /**
//     * 记录错误日志
//     */
//    LongAdder failureStat = new LongAdder();
//    Flux<String> flux =
//            Flux.just("unknown")
//                    .flatMap(k -> callExternalService(k))
//                    //记录错误日志
//                    //然后回调错误处理方法。
//                    .doOnError(e -> {
//                        failureStat.increment();
//                        log("uh oh, falling back, service failed for key " + k);
//                    })
//                    .onErrorResume(e -> getFromCache(k));
//
//
//    /**
//     * 使用资源和 try-catch 代码块
//     */
//    AtomicBoolean isDisposed = new AtomicBoolean();
//    Disposable disposableInstance = new Disposable() {
//        @Override
//        public void dispose() {
//            isDisposed.set(true);
//        }
//
//        @Override
//        public String toString() {
//            return "DISPOSABLE";
//        }
//    };
//
//    /**
//     * 第一个 lambda 生成资源，这里我们返回模拟的（mock） Disposable。
//     * 第二个 lambda 处理资源，返回一个 Flux<T>。
//     * 第三个 lambda 在 2) 中的资源 Flux 终止或取消的时候，用于清理资源。
//     * 在订阅或执行流序列之后， isDisposed 会置为 true。
//     */
//    Flux<String> flux =
//            Flux.using(
//                    () -> disposableInstance,
//                    disposable -> Flux.just(disposable.toString()),
//                    Disposable::dispose
//            );
//
//    //另一方面， doFinally 在序列终止（无论是 onComplete、`onError`还是取消）的时候被执行， 并且能够判断是什么类型
//    //take(1) 能够在发出 1 个元素后取消流。
//
//    LongAdder statsCancel = new LongAdder();
//
//    Flux<String> flux =
//            Flux.just("foo", "bar")
//                    .doFinally(type -> {
//                        if (type == SignalType.CANCEL)
//                            statsCancel.increment();
//                    })
//                    .take(1);
//
//    /**
//     * 演示终止方法 onError
//     *
//     * tick 0
//     * tick 1
//     * tick 2
//     * Uh oh
//     */
//    Flux<String> flux =
//            Flux.interval(Duration.ofMillis(250))
//                    .map(input -> {
//                        if (input < 3) return "tick " + input;
//                        throw new RuntimeException("boom");
//                    })
//                    .onErrorReturn("Uh oh");
//
//            flux.subscribe(System.out::println);
//            Thread.sleep(2100);
//
//    /**
//     *
//     * 重试
//     * 还有一个用于错误处理的操作符你可能会用到，就是 retry，见文知意，用它可以对出现错误的序列进行重试。
//     *
//     * 问题是它对于上游 Flux 是基于重订阅（re-subscribing）的方式。这实际上已经一个不同的序列了， 发出错误信号的序列仍然是终止了的
//     *
//     *
//     * 	elapsed 会关联从当前值与上个值发出的时间间隔（译者加：如下边输出的内容中的 259/249/251…​）。
//     * 我们还是要看一下 onError 时的内容。
//     * 确保我们有足够的时间可以进行 4x2 次 tick。
//     * 输出如下：
//     *
//     * 259,tick 0
//     * 249,tick 1
//     * 251,tick 2
//     * 506,tick 0         第四次的重试，重新获取上游的流；
//     * 248,tick 1
//     * 253,tick 2
//     * java.lang.RuntimeException: boom
//     *
//     *
//     * 个新的 interval 从 tick 0 开始。多出来的 250ms 间隔来自于第 4 次 tick，
//     * 就是导致出现异常并执行 retry 的那次（译者加：我在机器上测试的时候 elapsed “显示”的时间间隔没有加倍，但是确实有第 4 次的间隔）。
//     */
//        Flux.interval(Duration.ofMillis(250))
//                .map(input -> {
//                if (input < 3) return "tick " + input;
//                throw new RuntimeException("boom");
//            })
//                    .elapsed()
//            .retry(1)
//            .subscribe(System.out::println, System.err::println);
//
//        Thread.sleep(2100);
//
//    /**
//     *
//     * 还有一个“高配版”的 retry （retryWhen），它使用一个伴随（"companion"） Flux 来判断对某次错误是否要重试。这个伴随 Flux 是由操作符创建的，但是由开发者包装它， 从而实现对重试操作的配置。
//     *
//     * 这个伴随 Flux 是一个 Flux<Throwable>，它作为 retryWhen 的唯一参数被传递给一个 Function，你可以定义这个 Function 并让它返回一个新的 Publisher<?>。重试的循环 会这样运行：
//     *
//     * 每次出现错误，错误信号会发送给伴随 Flux，后者已经被你用 Function 包装。
//     *
//     * 如果伴随 Flux 发出元素，就会触发重试。
//     *
//     * 如果伴随 Flux 完成（complete），重试循环也会停止，并且原始序列也会 完成（complete）。
//     *
//     * 如果伴随 Flux 产生一个错误，重试循环停止，原始序列也停止 或 完成，并且这个错误会导致 原始序列失败并终止。
//     */
//    Flux<String> flux = Flux
//            // 持续产生错误。
//            .<String>error(new IllegalArgumentException())
//            //    在 retry 之前 的 doOnError 可以让我们看到错误。
//            .doOnError(System.out::println)
//            //    这里，我们认为前 3 个错误是可以重试的（take(3)），再有错误就放弃。
//            .retryWhen(companion -> companion.take(3));
//
//    //事实上，上边例子最终得到的是一个 空的 Flux，但是却 成功 完成了。
//    // 反观对同一个 Flux 调用 retry(3) 的话，最终是以最后一个 error 终止 Flux，故而 retryWhen 与之不同。
//    //实现同样的效果需要一些额外的技巧：
//
//    /**
//     *
//     *     技巧一：使用 zip 和一个“重试个数 + 1”的 range。
//     *     zip 方法让你可以在对重试次数计数的同时，仍掌握着原始的错误（error）。
//     *     允许三次重试，小于 4 的时候发出一个值。
//     *     为了使序列以错误结束。我们将原始异常在三次重试之后抛出。
//     */
//
//    Flux<String> flux =
//            Flux.<String>error(new IllegalArgumentException())
//                    .retryWhen(companion -> companion
//                            .zipWith(Flux.range(1, 4),
//                                    (error, index) -> {
//                                        if (index < 4) return index;
//                                        else throw Exceptions.propagate(error);
//                                    })
//                    );
//
//
//
//}
//
//
//
//
////例如这个监听器
//interface MyEventListener<T> {
//    void onDataChunk(List<T> chunk);
//
//    void processComplete();
//}
//
// interface SingleThreadEventListener<T> {
//    void onDataChunk(List<T> chunk);
//
//    void processComplete();
//
//    void processError(Throwable e);
//}
//
//interface  MyMessageListener<T> {
//    void onMessage(List<String> messages);
//}
//
//@Data
//class TreeVO {
//    public String id;
//    public String parentId;
//    public String name;
//    public List<TreeVO> childrenies;
//}
//
//
////这个类提供了一些 hook 方法，我们可以通过重写它们来调整 subscriber 的行为。
//// 默认情况下，它会触发一个无限个数的请求，但是当你想自定义请求元素的个数的时候，扩展 BaseSubscriber
//class SampleSubscriber<T> extends BaseSubscriber<T> {
//
//    public void hookOnSubscribe(Subscription subscription) {
//        System.out.println("Subscribed");
//        request(1);
//    }
//
//    public void hookOnNext(T value) {
//        System.out.println(value);
//        request(1);
//    }
}

