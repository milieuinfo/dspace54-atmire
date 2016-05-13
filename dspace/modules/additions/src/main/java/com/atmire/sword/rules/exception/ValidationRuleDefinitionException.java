package com.atmire.sword.rules.exception;

/**
 * Exception that is thrown when something is wrong the with Validation Rule definition file (config/item-validation-rules.xml)
 */
public class ValidationRuleDefinitionException extends Exception {

    public ValidationRuleDefinitionException(final String message, final Throwable ex) {
        super(message, ex);
    }

    public ValidationRuleDefinitionException(final String message) {
        super(message);
    }
}
