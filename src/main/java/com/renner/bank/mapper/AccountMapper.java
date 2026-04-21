package com.renner.bank.mapper;

import com.renner.bank.domain.Account;
import com.renner.bank.dto.AccountResponse;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getId(), account.getName(), account.getBalance());
    }
}
