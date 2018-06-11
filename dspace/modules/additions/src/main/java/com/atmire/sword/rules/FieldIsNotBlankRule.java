package com.atmire.sword.rules;

import java.util.*;
import static org.apache.commons.collections.CollectionUtils.*;
import org.apache.commons.lang3.*;
import org.dspace.content.*;

/**
 * Validation rule that will check if a field has a non-blank value.
 */
public class FieldIsNotBlankRule extends AbstractFieldCheckRule {

    private String checkedValue = null;

    public FieldIsNotBlankRule(final String fieldDescription, final String metadataField) {
        super(fieldDescription, metadataField);
    }

    @Override
    protected boolean checkFieldValues(final List<Metadatum> fieldValueList) {
        if (isEmpty(fieldValueList) || isBlank(fieldValueList.get(0))) {
            addViolationDescription("The %s field has a blank value", fieldDescription);
            return false;
        } else {
            checkedValue = fieldValueList.get(0).value;
            return true;
        }
    }


    protected String getRuleDescriptionCompliant() {
        return String.format(
                "the %s field (%s) is filled in",
                fieldDescription,
                metadataFieldToCheck
        );
    }

    protected String getRuleDescriptionViolation() {
        return String.format(
                "the %s field (%s) must be filled in",
                fieldDescription,
                metadataFieldToCheck
        );
    }

    public static boolean isBlank(Metadatum dcValue) {
        return dcValue == null || StringUtils.isBlank(dcValue.value);
    }
}
