package com.renner.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renner.bank.dto.TransferRequest;
import com.renner.bank.dto.TransactionResponse;
import com.renner.bank.exception.InsufficientFundsException;
import com.renner.bank.exception.TransferNotFoundException;
import com.renner.bank.exception.TransferToSameAccountException;
import com.renner.bank.exception.GlobalExceptionHandler;
import com.renner.bank.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock private TransactionService transactionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new TransactionController(transactionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void shouldCreateTransfer() throws Exception {
        var sourceId = UUID.randomUUID();
        var destinationId = UUID.randomUUID();
        var transactionId = UUID.randomUUID();
        var request = new TransferRequest(sourceId, destinationId, new BigDecimal("100.00"));
        var response = new TransactionResponse(
                transactionId,
                sourceId,
                "Alice",
                destinationId,
                "Bob",
                new BigDecimal("100.00"),
                "COMPLETED",
                LocalDateTime.now()
        );
        when(transactionService.transfer(request)).thenReturn(response);

        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void shouldReturnBadRequestWhenTransferPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccountId": null,
                                  "destinationAccountId": null,
                                  "amount": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.sourceAccountId").value("O campo `sourceAccountId` é obrigatório."))
                .andExpect(jsonPath("$.errors.destinationAccountId").value("O campo `destinationAccountId` é obrigatório."))
                .andExpect(jsonPath("$.errors.amount").value("O campo `amount` deve ser maior ou igual a 0.01."));
    }

    @Test
    void shouldReturnBadRequestWhenTransferIsToSameAccount() throws Exception {
        var accountId = UUID.randomUUID();
        var request = new TransferRequest(accountId, accountId, new BigDecimal("100.00"));
        when(transactionService.transfer(request)).thenThrow(new TransferToSameAccountException());

        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Source and destination accounts must be different"));
    }

    @Test
    void shouldReturnUnprocessableEntityWhenBalanceIsInsufficient() throws Exception {
        var sourceId = UUID.randomUUID();
        var destinationId = UUID.randomUUID();
        var request = new TransferRequest(sourceId, destinationId, new BigDecimal("100.00"));
        when(transactionService.transfer(request))
                .thenThrow(new InsufficientFundsException(sourceId, new BigDecimal("10.00"), new BigDecimal("100.00")));

        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Insufficient funds in account " + sourceId + ": balance=10.00, requested=100.00"));
    }

    @Test
    void shouldReturnTransferById() throws Exception {
        var sourceId = UUID.randomUUID();
        var destinationId = UUID.randomUUID();
        var transactionId = UUID.randomUUID();
        var response = new TransactionResponse(
                transactionId,
                sourceId,
                "Alice",
                destinationId,
                "Bob",
                new BigDecimal("100.00"),
                "COMPLETED",
                LocalDateTime.now()
        );
        when(transactionService.findById(transactionId)).thenReturn(response);

        mockMvc.perform(get("/transaction/{id}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.sourceAccountName").value("Alice"))
                .andExpect(jsonPath("$.destinationAccountName").value("Bob"));
    }

    @Test
    void shouldReturnNotFoundWhenTransferDoesNotExist() throws Exception {
        var transactionId = UUID.randomUUID();
        when(transactionService.findById(transactionId)).thenThrow(new TransferNotFoundException(transactionId));

        mockMvc.perform(get("/transaction/{id}", transactionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Transfer not found for id: " + transactionId));
    }
}
