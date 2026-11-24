package com.aikids.application.port.out;

import java.util.function.Supplier;

public interface TransactionPort {

    <T> T run(Supplier<T> work);

    default void run(Runnable work) {
        run(() -> {
            work.run();
            return null;
        });
    }
}
