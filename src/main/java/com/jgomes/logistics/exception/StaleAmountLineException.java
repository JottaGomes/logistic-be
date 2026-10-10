package com.jgomes.logistics.exception;

import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Raised when a correction was based on a version of the line that is no longer
 * current: someone else changed it after the caller read it.
 *
 * Extends Spring's OptimisticLockingFailureException so it is handled together
 * with the conflicts Hibernate raises itself for entities carrying {@code @Version}.
 */
public class StaleAmountLineException extends OptimisticLockingFailureException {

    public StaleAmountLineException(String kind, Long id) {
        super("The " + kind + " with id " + id + " was changed by someone else. Reload it and apply the correction again.");
    }
}
