package com.renner.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renner.bank.domain.Account;
import com.renner.bank.dto.AccountRequest;
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
class AccountControllerIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired AccountRepository accountRepository;
    @Autowired TransactionRepository transactionRepository;
    @MockitoBean NotificationService notificationService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void shouldCreateAccountAndReturn201() throws Exception {
        AccountRequest request = new AccountRequest("João Silva", new BigDecimal("1000.00"));

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    void shouldReturn400WhenAccountRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": " ", "initialBalance": -1 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("O campo `name` é obrigatório."))
                .andExpect(jsonPath("$.errors.initialBalance").value("O campo `initialBalance` não pode ser negativo."));
    }

    @Test
    void shouldListAccountsOrderedByName() throws Exception {
        accountRepository.save(new Account("Zara", new BigDecimal("100.00")));
        accountRepository.save(new Account("Alice", new BigDecimal("200.00")));

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Alice"))
                .andExpect(jsonPath("$.content[1].name").value("Zara"));
    }

    @Test
    void shouldReturnAccountById() throws Exception {
        Account account = accountRepository.save(new Account("Alice", new BigDecimal("500.00")));

        mockMvc.perform(get("/accounts/{id}", account.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(account.getId().toString()))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.balance").value(500.00));
    }

    @Test
    void shouldReturn404WhenAccountDoesNotExist() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/accounts/{id}", randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account not found: " + randomId));
    }

    @Test
    void shouldReturnAccountStatement() throws Exception {
        Account source = accountRepository.save(new Account("Alice", new BigDecimal("1000.00")));
        Account destination = accountRepository.save(new Account("Bob", new BigDecimal("500.00")));

        mockMvc.perform(post("/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferRequest(source.getId(), destination.getId(), new BigDecimal("100.00")))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/{id}/transaction", source.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.content[0].amount").value(100.00));
    }

    @Test
    void shouldReturn404WhenStatementAccountDoesNotExist() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/accounts/{id}/transaction", randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account not found: " + randomId));
    }
}
