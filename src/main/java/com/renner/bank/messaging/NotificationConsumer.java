package com.renner.bank.messaging;

import com.renner.bank.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final RestClient restClient;

    @Value("${notification.url}")
    private String notificationUrl;

    public NotificationConsumer(RestClient restClient) {
        this.restClient = restClient;
    }

    @RabbitListener(queues = "${rabbitmq.queue}")
    public void consume(NotificationRequest payload) {
        try {
            restClient.post()
                    .uri(notificationUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[NOTIFICATION] Transfer of R${} from {} to {} notified successfully",
                    payload.amount(), payload.sourceName(), payload.destinationName());
        } catch (RestClientException e) {
            log.error("[NOTIFICATION] Failed to notify transfer of {} from {} to {}: {}. Routing to DLQ.",
                    payload.amount(), payload.sourceName(), payload.destinationName(), e.getMessage());
            throw e;
        }
    }
}
