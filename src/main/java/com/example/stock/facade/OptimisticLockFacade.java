package com.example.stock.facade;

import com.example.stock.service.StockService;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
public class OptimisticLockFacade {

    private StockService stockService;

    public OptimisticLockFacade(StockService stockService) {
        this.stockService = stockService;
    }

    public void decreaseWithOptimisticLockFacade(Long id, Long quantity) {
        while (true) {
            try {
                stockService.decreaseWithOptimisticLock(id, quantity);
                break;
            } catch (ObjectOptimisticLockingFailureException e) {
                System.out.println("optimistic lock");
            }
        }
    }
}
