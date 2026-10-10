package com.jgomes.logistics.exception;

/** Raised when an income or cost line does not exist on the shipment it was addressed through. */
public class AmountLineNotFoundException extends RuntimeException {

    public AmountLineNotFoundException(String kind, Long id, String reference) {
        super("No " + kind + " with id " + id + " on shipment " + reference);
    }
}
