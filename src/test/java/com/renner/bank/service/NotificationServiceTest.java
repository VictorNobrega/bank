package com.renner.bank.service;

import com.renner.bank.dto.NotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private NotificationService notificationService;

    private final UUID sourceId = UUID.randomUUID();
    private final UUID destinationId = UUID.randomUUID();
    private final BigDecimal amount = new BigDecimal("200.00");

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(rabbitTemplate);
        ReflectionTestUtils.setField(notificationService, "exchange", "bank.notifications");
        ReflectionTestUtils.setField(notificationService, "routingKey", "notification");
    }

    @Test
    void shouldPublishNotificationSuccessfully() {
        var expected = new NotificationRequest(sourceId, "Alice", destinationId, "Bob", amount);

        notificationService.notify(sourceId, "Alice", destinationId, "Bob", amount);

        verify(rabbitTemplate).convertAndSend("bank.notifications", "notification", expected);
    }

    @Test
    void shouldNotPropagateExceptionWhenPublishFails() {
        doThrow(new AmqpException("connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatCode(() ->
                notificationService.notify(sourceId, "Alice", destinationId, "Bob", amount)
        ).doesNotThrowAnyException();
    }
}
