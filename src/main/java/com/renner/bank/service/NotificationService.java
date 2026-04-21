package com.renner.bank.service;

import com.renner.bank.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final RestClient restClient;

    @Value("${notification.url}")
    private String notificationUrl;

    public NotificationService(RestClient restClient) {
        this.restClient = restClient;
    }

    @Async
    public void notify(UUID sourceId, String sourceName,
                       UUID destinationId, String destinationName,
                       BigDecimal amount) {
        try {
            NotificationRequest payload = new NotificationRequest(
                    sourceId, sourceName, destinationId, destinationName, amount
            );
            restClient.post()
                    .uri(notificationUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[NOTIFICATION] Transfer of R${} from {} to {} notified successfully",
                    amount, sourceName, destinationName);
        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed to notify transfer of {} from {} to {}: {}",
                    amount, sourceId, destinationId, e.getMessage());
        }
    }
}
