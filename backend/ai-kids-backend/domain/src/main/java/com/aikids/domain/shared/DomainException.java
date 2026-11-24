package com.aikids.domain.shared;

/** Regla de negocio incumplida. El código es estable; el mensaje es para la persona. */
public class DomainException extends RuntimeException {

    private final String code;

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
