package com.renner.bank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NotificationServiceTest {

    private static final String NOTIFICATION_URL = "https://util.devi.tools/api/v1/notify";

    private MockRestServiceServer server;
    private NotificationService notificationService;

    private final UUID sourceId = UUID.randomUUID();
    private final UUID destinationId = UUID.randomUUID();
    private final BigDecimal amount = new BigDecimal("200.00");

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var restClient = builder.build();
        notificationService = new NotificationService(restClient);
        ReflectionTestUtils.setField(notificationService, "notificationUrl", NOTIFICATION_URL);
    }

    @Test
    void shouldSendNotificationSuccessfully() {
        server.expect(requestTo(NOTIFICATION_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        assertThatCode(() ->
                notificationService.notify(sourceId, "Alice", destinationId, "Bob", amount)
        ).doesNotThrowAnyException();

        server.verify();
    }

    @Test
    void shouldNotPropagateExceptionWhenNotificationFails() {
        server.expect(requestTo(NOTIFICATION_URL))
                .andRespond(withServerError());

        assertThatCode(() ->
                notificationService.notify(sourceId, "Alice", destinationId, "Bob", amount)
        ).doesNotThrowAnyException();
    }
}
