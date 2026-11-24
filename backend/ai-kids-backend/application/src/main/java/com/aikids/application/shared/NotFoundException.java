package com.aikids.application.shared;

import com.aikids.domain.shared.DomainException;

public class NotFoundException extends DomainException {

    public NotFoundException(String what) {
        super("NO_ENCONTRADO", "No se encontró " + what + ".");
    }
}
