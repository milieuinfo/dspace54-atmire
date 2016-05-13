package com.atmire.sword.rules;

import java.util.*;
import org.apache.commons.lang.*;
import org.dspace.content.*;
import org.dspace.core.*;

/**
 * Abstract implementation of a compliance rule that checks a certain field
 */
public abstract class AbstractFieldCheckRule extends AbstractComplianceRule {

    protected String metadataFieldToCheck;
    protected String fieldDescription;

    public AbstractFieldCheckRule(final String fieldDescription, final String metadataField) {
        this.fieldDescription = StringUtils.trimToEmpty(fieldDescription);
        this.metadataFieldToCheck = StringUtils.trimToNull(metadataField);
    }

    @Override
    protected boolean doValidationAndBuildDescription(final Context context, final Item item) {
        if (metadataFieldToCheck == null) {
            addViolationDescription("Cannot validate a blank field");
            return false;
        } else {
            try {
                List<Metadatum> fieldValueList = getMetadata(context, item, metadataFieldToCheck);
                return checkFieldValues(fieldValueList);

            } catch(IllegalArgumentException ex) {
                addViolationDescription("The metadata field %s is invalid because it has too few tokens", metadataFieldToCheck);
                return false;
            }
        }
    }

    abstract protected boolean checkFieldValues(final List<Metadatum> fieldValueList);
}
