package com.atmire.sword.rules;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Metadatum;

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
            addViolationDescription("het %s veld heeft een lege waarde", fieldDescription);
            return false;
        } else {
            checkedValue = fieldValueList.get(0).value;
            return true;
        }
    }


    protected String getRuleDescriptionCompliant() {
        return String.format(
                "het %s veld (%s) is ingevuld",
                fieldDescription,
                metadataFieldToCheck
        );
    }

    protected String getRuleDescriptionViolation() {
        return String.format(
                "het %s veld (%s) moet ingevuld worden",
                fieldDescription,
                metadataFieldToCheck
        );
    }

    public static boolean isBlank(Metadatum dcValue) {
        return dcValue == null || StringUtils.isBlank(dcValue.value);
    }
}
