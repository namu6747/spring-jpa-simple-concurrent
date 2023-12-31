package com.example.stock.facade;

import com.example.stock.service.StockService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedissonLockFacade {

    private RedissonClient redissonClient;
    private StockService stockService;

    public RedissonLockFacade(RedissonClient redissonClient, StockService stockService) {
        this.redissonClient = redissonClient;
        this.stockService = stockService;
    }

    public void decrease(Long key, Long quantity) {
        RLock lock = redissonClient.getLock(key.toString());

        try {
            boolean lockEnable = lock.tryLock(5, 1, TimeUnit.SECONDS);

            if (!lockEnable) {
                System.out.println("lock 획득 실패");
                return;
            }

            stockService.decrease(key, quantity);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}
