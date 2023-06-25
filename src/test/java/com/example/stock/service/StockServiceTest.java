package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.facade.NamedLockFacade;
import com.example.stock.facade.OptimisticLockFacade;
import com.example.stock.repository.StockRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@SpringBootTest
class StockServiceTest {

    @Autowired
    StockService stockService;
    @Autowired
    OptimisticLockFacade optimisticLockFacade;
    @Autowired
    NamedLockFacade namedLockFacade;
    @Autowired
    StockRepository stockRepository;

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

    @Test
    void execute_concurrent_requests_100_times() throws Exception {

        int threadCount = 8;
        int taskCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(taskCount);

        IntStream.range(0, taskCount).forEach(i -> {
            executorService.submit(() -> {
                try {
                    //optimisticLockFacade.decreaseWithOptimisticLockFacade(1L, 1L);
                    namedLockFacade.decrease(1L, 1L);
                } finally {
                    latch.countDown();
                }
            });
        });

        latch.await();

        Stock stock = stockRepository.findByProductId(1L).orElseThrow();
        Assertions.assertThat(stock.getQuantity()).isEqualTo(0L);
    }
}

