package com.adrianojlt.logistics.exception;

/** Raised when the requested shipment does not exist — alternative flow I. */
public class ShipmentNotFoundException extends RuntimeException {

    public ShipmentNotFoundException(String reference) {
        super("No shipment found with reference " + reference);
    }
}
