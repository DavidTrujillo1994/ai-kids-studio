package com.aikids.domain.shared;

public class InvalidStateException extends DomainException {

    public InvalidStateException(String message) {
        super("ESTADO_INVALIDO", message);
    }
}
