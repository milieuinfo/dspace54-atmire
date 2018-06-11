package com.atmire.sword.rules;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Metadatum;

import java.util.List;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

/**
 * Validation rule that will check if a field has a non-blank value.
 */
public class FieldIsBlankRule extends AbstractFieldCheckRule {

    private String checkedValue = null;

    public FieldIsBlankRule(final String fieldDescription, final String metadataField) {
        super(fieldDescription, metadataField);
    }

    @Override
    protected boolean checkFieldValues(final List<Metadatum> fieldValueList) {
        if (isEmpty(fieldValueList) || isBlank(fieldValueList.get(0))) {
            return true;
        } else {
            addViolationDescription("Field %s has a value", fieldDescription);
            checkedValue = fieldValueList.get(0).value;
            return false;
        }
    }

    protected String getRuleDescriptionCompliant() {
        return String.format(
                "the %s field (%s) is blank",
                fieldDescription,
                metadataFieldToCheck
        );
    }

    protected String getRuleDescriptionViolation() {
        return String.format(
                "the %s field (%s) must not be filled in",
                fieldDescription,
                metadataFieldToCheck
        );
    }

    public static boolean isBlank(Metadatum dcValue) {
        return dcValue == null || StringUtils.isBlank(dcValue.value);
    }
}
