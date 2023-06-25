package com.example.stock.facade;

import com.example.stock.repository.RedisLockRepository;
import com.example.stock.service.StockService;
import org.springframework.stereotype.Component;

@Component
public class LettuceLockFacade {
    private RedisLockRepository redisLockRepository;
    private StockService stockService;

    public LettuceLockFacade(RedisLockRepository redisLockRepository, StockService stockService) {
        this.redisLockRepository = redisLockRepository;
        this.stockService = stockService;
    }

    public void decrease(Long key, Long quantity) {
        while (!redisLockRepository.lock(key)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            stockService.decrease(key, quantity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            redisLockRepository.unlock(key);
        }

    }

}
