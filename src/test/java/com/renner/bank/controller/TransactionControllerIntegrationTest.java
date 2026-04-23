package com.renner.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renner.bank.domain.Account;
import com.renner.bank.dto.TransferRequest;
import com.renner.bank.repository.AccountRepository;
import com.renner.bank.repository.TransactionRepository;
import com.renner.bank.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class TransactionControllerIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired AccountRepository accountRepository;
    @Autowired TransactionRepository transactionRepository;
    @MockitoBean NotificationService notificationService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private Account source;
    private Account destination;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        source = accountRepository.save(new Account("Alice", new BigDecimal("1000.00")));
        destination = accountRepository.save(new Account("Bob", new BigDecimal("500.00")));
    }

    @Test
    void shouldTransferSuccessfullyAndReturn201() throws Exception {
        var request = new TransferRequest(source.getId(), destination.getId(), new BigDecimal("200.00"));

        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(200.00))
                .andExpect(jsonPath("$.sourceAccountName").value("Alice"))
                .andExpect(jsonPath("$.destinationAccountName").value("Bob"));
    }

    @Test
    void shouldReturn422OnInsufficientFunds() throws Exception {
        var request = new TransferRequest(source.getId(), destination.getId(), new BigDecimal("9999.00"));

        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("Insufficient funds in account " + source.getId())));
    }

    @Test
    void shouldReturn400OnSameAccountTransfer() throws Exception {
        var request = new TransferRequest(source.getId(), source.getId(), new BigDecimal("100.00"));

        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Source and destination accounts must be different"));
    }

    @Test
    void shouldReturn404WhenAccountNotFound() throws Exception {
        var unknownId = UUID.randomUUID();
        var request = new TransferRequest(unknownId, destination.getId(), new BigDecimal("100.00"));

        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account not found: " + unknownId));
    }

    @Test
    void shouldReturn400WhenTransferPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sourceAccountId": null, "destinationAccountId": null, "amount": 0 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.sourceAccountId").value("O campo `sourceAccountId` é obrigatório."))
                .andExpect(jsonPath("$.errors.destinationAccountId").value("O campo `destinationAccountId` é obrigatório."));
    }

    @Test
    void shouldReturnTransactionById() throws Exception {
        var request = new TransferRequest(source.getId(), destination.getId(), new BigDecimal("100.00"));
        var responseJson = mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var transactionId = objectMapper.readTree(responseJson).get("transactionId").asText();

        mockMvc.perform(get("/transaction/{id}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.sourceAccountName").value("Alice"));
    }

    @Test
    void shouldReturn404WhenTransactionNotFound() throws Exception {
        var randomId = UUID.randomUUID();

        mockMvc.perform(get("/transaction/{id}", randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Transferência não encontrada para o id: " + randomId));
    }
}
