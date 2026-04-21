package com.renner.bank.exception;

public class TransferToSameAccountException extends RuntimeException {
    public TransferToSameAccountException() {
        super("Source and destination accounts must be different");
    }
}
