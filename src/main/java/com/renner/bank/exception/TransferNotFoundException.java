package com.renner.bank.exception;

import java.util.UUID;

public class TransferNotFoundException extends RuntimeException {

    public TransferNotFoundException(UUID transferId) {
        super("Transferência não encontrada para o id: " + transferId);
    }
}
