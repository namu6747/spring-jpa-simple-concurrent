package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.facade.LettuceLockFacade;
import com.example.stock.facade.NamedLockFacade;
import com.example.stock.facade.OptimisticLockFacade;
import com.example.stock.facade.RedissonLockFacade;
import com.example.stock.repository.StockRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@SpringBootTest
class StockServiceTest {

    @Autowired
    StockService stockService;
    @Autowired
    StockRepository stockRepository;
    @Autowired
    OptimisticLockFacade optimisticLockFacade;
    @Autowired
    NamedLockFacade namedLockFacade;
    @Autowired
    LettuceLockFacade lettuceLockFacade;
    @Autowired
    RedissonLockFacade redissonLockFacade;

    @BeforeEach
    void beforeEach() {
        Stock stock = new Stock(1L, 100L);
        stockRepository.save(stock);
    }

    @AfterEach
    void afterEach() {
        stockRepository.deleteAll();
    }

    @Test
    void stock_decrease() {
        stockService.decrease(1L, 1L);
        Stock findStock = stockRepository.findById(1L).orElseThrow();
        Assertions.assertThat(findStock.getQuantity()).isEqualTo(99L);
    }

    Consumer lettuceConsumer = (o) -> lettuceLockFacade.decrease(1L, 1L);
    Consumer optimisticConsumer = (o) -> optimisticLockFacade.decreaseWithOptimisticLockFacade(1L, 1L);
    Consumer pessimisticConsumer = (o) -> stockService.decreaseWithPessimisticLock(1L, 1L);
    Consumer namedConsumer = (o) -> namedLockFacade.decrease(1L, 1L);
    Consumer redissonConsumer = (o) -> redissonLockFacade.decrease(1L, 1L);
    Consumer synchronizedConsumer = (o) -> stockService.decreaseWithSynchronized(1L, 1L);

    @Test
    void execute_concurrent_requests_100_times() throws Exception {

        final StopWatch stopWatch = new StopWatch();

        int threadCount = 8;
        int taskCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(taskCount);

        stopWatch.start();
        IntStream.range(0, taskCount).forEach(i -> {
            executorService.submit(() -> {
                try {
                    synchronizedConsumer.accept(null);
                } finally {
                    latch.countDown();
                }
            });
        });

        latch.await();
        stopWatch.stop();
        long readTime = stopWatch.getLastTaskTimeMillis();
        System.out.println("readTime = " + readTime);


        Stock stock = stockRepository.findByProductId(1L).orElseThrow();
        Assertions.assertThat(stock.getQuantity()).isEqualTo(0L);
    }
}

