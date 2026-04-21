package com.renner.bank.controller;

import com.renner.bank.dto.TransferRequest;
import com.renner.bank.dto.TransactionResponse;
import com.renner.bank.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/transaction")
@Tag(name = "Transaction", description = "Fund transfers between accounts")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transfer funds between two accounts")
    public TransactionResponse transfer(@Valid @RequestBody TransferRequest request) {
        return transactionService.transfer(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transfer by ID")
    public TransactionResponse findById(@PathVariable UUID id) {
        return transactionService.findById(id);
    }
}
