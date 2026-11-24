package com.aikids.domain.script;

/** step es el índice del paso, o -1 si el problema es del guion completo. */
public record ScriptProblem(int step, String message) {
}
