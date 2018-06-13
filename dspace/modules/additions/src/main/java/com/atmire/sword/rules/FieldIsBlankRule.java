package com.atmire.sword.rules;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Metadatum;

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
            addViolationDescription("het veld %s heeft een waarde", fieldDescription);
            checkedValue = fieldValueList.get(0).value;
            return false;
        }
    }

    protected String getRuleDescriptionCompliant() {
        return String.format(
                "het %s veld (%s) is leeg",
                fieldDescription,
                metadataFieldToCheck
        );
    }

    protected String getRuleDescriptionViolation() {
        return String.format(
                "het %s veld (%s) mag niet ingevuld worden",
                fieldDescription,
                metadataFieldToCheck
        );
    }

    public static boolean isBlank(Metadatum dcValue) {
        return dcValue == null || StringUtils.isBlank(dcValue.value);
    }
}
