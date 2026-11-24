package com.aikids.domain.script;

import com.aikids.domain.shared.DomainException;
import java.util.List;

public class InvalidScriptException extends DomainException {

    private final transient List<ScriptProblem> problems;

    public InvalidScriptException(List<ScriptProblem> problems) {
        super("GUION_INVALIDO", "El guion tiene " + problems.size() + " detalle(s) por corregir.");
        this.problems = List.copyOf(problems);
    }

    public List<ScriptProblem> problems() {
        return problems;
    }
}
