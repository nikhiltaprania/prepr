package com.springrag.common.tx;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Runs a block of persistence work inside a single short transaction, programmatically.
 *
 * Used by AI services so the (slow) LLM call happens OUTSIDE any transaction while the
 * resulting writes are still committed atomically — i.e. load → call model (no tx) →
 * {@code TransactionRunner.run(persist)}. This avoids holding a JPA connection open for
 * the duration of model inference.
 */
@Component
public class TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    public TransactionRunner(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void run(Runnable work) {
        transactionTemplate.executeWithoutResult(status -> work.run());
    }

    public <T> T call(Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }
}
