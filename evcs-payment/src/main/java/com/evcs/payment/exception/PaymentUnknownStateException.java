package com.evcs.payment.exception;

/**
 * Represents a transient failure during payment creation where the final state is unknown.
 * <p>
 * In this case the system must NOT automatically retry creating a new payment, and should instead
 * persist an intermediate state and reconcile via query/polling.
 */
public class PaymentUnknownStateException extends RuntimeException {

    public PaymentUnknownStateException(String message) {
        super(message);
    }

    public PaymentUnknownStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
