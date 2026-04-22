package com.renner.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renner.bank.dto.AccountRequest;
import com.renner.bank.dto.AccountResponse;
import com.renner.bank.dto.TransactionResponse;
import com.renner.bank.dto.pagination.PaginatedResponse;
import com.renner.bank.exception.AccountNotFoundException;
import com.renner.bank.exception.GlobalExceptionHandler;
import com.renner.bank.service.AccountService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock private AccountService accountService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AccountController(accountService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void shouldCreateAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        AccountRequest request = new AccountRequest("João Silva", new BigDecimal("1000.00"));
        AccountResponse response = new AccountResponse(accountId, "João Silva", new BigDecimal("1000.00"));
        when(accountService.create(request)).thenReturn(response);

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    void shouldReturnBadRequestWhenCreateAccountPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "initialBalance": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("O campo `name` é obrigatório."))
                .andExpect(jsonPath("$.errors.initialBalance").value("O campo `initialBalance` não pode ser negativo."));
    }

    @Test
    void shouldListAccounts() throws Exception {
        PaginatedResponse<AccountResponse> response = new PaginatedResponse<>(
                List.of(new AccountResponse(UUID.randomUUID(), "Alice", new BigDecimal("100.00"))),
                1,
                1,
                1,
                0,
                20
        );
        when(accountService.findAll(0, 20)).thenReturn(response);

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Alice"));
    }

    @Test
    void shouldReturnAccountById() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.findById(accountId))
                .thenReturn(new AccountResponse(accountId, "Alice", new BigDecimal("50.00")));

        mockMvc.perform(get("/accounts/{id}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void shouldReturnNotFoundWhenAccountDoesNotExist() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.findById(accountId)).thenThrow(new AccountNotFoundException(accountId));

        mockMvc.perform(get("/accounts/{id}", accountId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account not found: " + accountId));
    }

    @Test
    void shouldReturnStatement() throws Exception {
        UUID accountId = UUID.randomUUID();
        PaginatedResponse<TransactionResponse> response = new PaginatedResponse<>(
                List.of(new TransactionResponse(
                        UUID.randomUUID(),
                        accountId,
                        "Alice",
                        UUID.randomUUID(),
                        "Bob",
                        new BigDecimal("10.00"),
                        "COMPLETED",
                        LocalDateTime.now()
                )),
                1,
                1,
                1,
                0,
                20
        );
        when(accountService.getStatement(accountId, 0, 20)).thenReturn(response);

        mockMvc.perform(get("/accounts/{id}/statement", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sourceAccountName").value("Alice"))
                .andExpect(jsonPath("$.content[0].destinationAccountName").value("Bob"));
    }

    @Test
    void shouldReturnNotFoundWhenStatementAccountDoesNotExist() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.getStatement(accountId, 0, 20)).thenThrow(new AccountNotFoundException(accountId));

        mockMvc.perform(get("/accounts/{id}/statement", accountId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account not found: " + accountId));
    }
}
