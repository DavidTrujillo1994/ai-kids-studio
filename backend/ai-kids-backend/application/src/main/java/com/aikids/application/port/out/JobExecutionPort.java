package com.aikids.application.port.out;

/** Ejecuta trabajos largos en segundo plano, de a uno para no agotar el computador. */
public interface JobExecutionPort {

    void submit(Runnable job);
}
