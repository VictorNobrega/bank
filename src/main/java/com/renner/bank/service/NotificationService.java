package com.renner.bank.service;

import com.renner.bank.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key}")
    private String routingKey;

    public NotificationService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void notify(UUID sourceId, String sourceName,
                       UUID destinationId, String destinationName,
                       BigDecimal amount) {
        try {
            var payload = new NotificationRequest(sourceId, sourceName, destinationId, destinationName, amount);
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            log.info("[NOTIFICATION] Transfer event published: {} -> {}, amount={}", sourceName, destinationName, amount);
        } catch (AmqpException e) {
            log.error("[NOTIFICATION] Failed to publish notification event: {}", e.getMessage());
        }
    }
}
