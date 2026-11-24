package com.aikids.domain.shared;

public class InvalidDataException extends DomainException {

    public InvalidDataException(String message) {
        super("DATO_INVALIDO", message);
    }
}
