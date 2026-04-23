package com.renner.bank.controller;

import com.renner.bank.dto.AccountRequest;
import com.renner.bank.dto.AccountResponse;
import com.renner.bank.dto.TransactionResponse;
import com.renner.bank.dto.pagination.PaginatedResponse;
import com.renner.bank.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    public AccountResponse create(@Valid @RequestBody AccountRequest request) {
        return accountService.create(request);
    }

    @GetMapping
    @Operation(summary = "List all accounts", description = "Results are sorted by the `name` field in ascending order.")
    @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully")
    public PaginatedResponse<AccountResponse> findAll(@RequestParam(defaultValue = "0") @Min(0) Integer page,
                                                      @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        return accountService.findAll(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account found"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public AccountResponse findById(@PathVariable UUID id) {
        return accountService.findById(id);
    }

    @GetMapping("/{id}/transaction")
    @Operation(summary = "Get account statement (paginated transactions)",
            description = "Results are sorted by the `createdAt` field in descending order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statement retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public PaginatedResponse<TransactionResponse> getTransactionByAccountId(@PathVariable UUID id,
                                                                            @RequestParam(defaultValue = "0") @Min(0) Integer page,
                                                                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        return accountService.getTransactionByAccountId(id, page, size);
    }
}
