package com.renner.bank.config;

import com.renner.bank.domain.Account;
import com.renner.bank.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(AccountRepository accountRepository,
                               TransactionTemplate transactionTemplate) {
        return args -> transactionTemplate.executeWithoutResult(status -> {
            if (accountRepository.count() > 0) return;

            accountRepository.save(new Account("Alice Souza", new BigDecimal("5000.00")));
            accountRepository.save(new Account("Bruno Lima", new BigDecimal("3000.00")));
            accountRepository.save(new Account("Carla Mendes", new BigDecimal("10000.00")));
            accountRepository.save(new Account("Diego Rocha", new BigDecimal("1500.00")));
        });
    }
}
