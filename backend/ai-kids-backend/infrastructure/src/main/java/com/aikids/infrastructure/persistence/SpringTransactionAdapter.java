package com.aikids.infrastructure.persistence;

import com.aikids.application.port.out.TransactionPort;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class SpringTransactionAdapter implements TransactionPort {

    private final TransactionTemplate template;

    SpringTransactionAdapter(PlatformTransactionManager manager) {
        this.template = new TransactionTemplate(manager);
    }

    @Override
    public <T> T run(Supplier<T> work) {
        return template.execute(status -> work.get());
    }
}
