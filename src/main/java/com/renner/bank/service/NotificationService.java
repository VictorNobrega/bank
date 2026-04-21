package com.renner.bank.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Async
    public void notify(UUID sourceId, String sourceName, UUID destinationId, String destinationName, BigDecimal amount) {
        try {
            log.info("[NOTIFICATION] {} sent R${} to {}", sourceName, amount, destinationName);
            log.info("[NOTIFICATION] {} received R${} from {}", destinationName, amount, sourceName);
            // In production: call external notification gateway (email/SMS/push)
        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed to notify transfer of {} from {} to {}: {}",
                    amount, sourceId, destinationId, e.getMessage());
        }
    }
}
