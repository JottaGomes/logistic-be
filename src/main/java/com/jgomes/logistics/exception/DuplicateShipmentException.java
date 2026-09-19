package com.jgomes.logistics.exception;

/** Raised when a shipment reference is already taken — it is the business key. */
public class DuplicateShipmentException extends RuntimeException {

    public DuplicateShipmentException(String reference) {
        super("A shipment with reference " + reference + " already exists");
    }
}
