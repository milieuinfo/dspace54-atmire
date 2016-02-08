package com.atmire.lne.exception;

/**
 * Exception that indicates that the LNE external handle metadata field is not set
 */
public class MetaDataFieldNotSetException extends Exception {
    public MetaDataFieldNotSetException(final String message) {
        super(message);
    }
}
