package com.atmire.sword.rules;

import org.apache.commons.lang.*;
import org.dspace.content.*;
import org.dspace.core.*;

/**
 * Validation rule that will check if a field has a non-blank value.
 */
public class FailOnPreconditionSuccessRule extends AbstractComplianceRule {

    protected String fieldDescription;

    public FailOnPreconditionSuccessRule(final String fieldDescription, final String metadataField) {
        this.fieldDescription = StringUtils.trimToEmpty(fieldDescription);
    }

    @Override
    protected String getRuleDescriptionCompliant() {
        return "Velden combinatie van " + fieldDescription + " is geldig";
    }

    @Override
    protected String getRuleDescriptionViolation() {
        return "Velden combinatie van " + fieldDescription + " is ongeldig";
    }

    @Override
    protected boolean doValidationAndBuildDescription(Context context, Item item) {
        addViolationDescription("Velden %s kunnen niet beiden een waarde hebben binnen eenzelfde item", fieldDescription);
        return false;
    }
}
