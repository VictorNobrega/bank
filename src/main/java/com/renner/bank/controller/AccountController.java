package com.renner.bank.controller;

import com.renner.bank.dto.AccountRequest;
import com.renner.bank.dto.AccountResponse;
import com.renner.bank.dto.TransactionResponse;
import com.renner.bank.dto.pagination.PaginatedResponse;
import com.renner.bank.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/accounts")
@Tag(name = "Accounts", description = "Account management")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new account")
    public AccountResponse create(@Valid @RequestBody AccountRequest request) {
        return accountService.create(request);
    }

    @GetMapping
    @Operation(summary = "List all accounts", description = "A ordenação é feita pelo campo `name` ascendente.")
    public PaginatedResponse<AccountResponse> findAll(@RequestParam(defaultValue = "0") @Min(0) Integer page,
                                                      @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        return accountService.findAll(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID")
    public AccountResponse findById(@PathVariable UUID id) {
        return accountService.findById(id);
    }

    @GetMapping("/{id}/statement")
    @Operation(summary = "Get account statement (paginated transactions)",
            description = "A ordenação é feita pelo campo `createdAt` decrescente.")
    public PaginatedResponse<TransactionResponse> getStatement(@PathVariable UUID id,
                                                               @RequestParam(defaultValue = "0") @Min(0) Integer page,
                                                               @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        return accountService.getStatement(id, page, size);
    }
}
