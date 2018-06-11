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
        return "succeeded on precondition";
    }

    @Override
    protected String getRuleDescriptionViolation() {
        return "failed on precondition";
    }

    @Override
    protected boolean doValidationAndBuildDescription(Context context, Item item) {
        addViolationDescription("Fields %s cannot both have a value in the metadata set of one item", fieldDescription);
        return false;
    }
}
