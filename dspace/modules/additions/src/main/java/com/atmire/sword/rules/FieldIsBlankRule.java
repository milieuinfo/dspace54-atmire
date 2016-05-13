package com.atmire.sword.rules;

import java.util.*;
import static org.apache.commons.collections.CollectionUtils.*;
import org.apache.commons.lang3.*;
import org.dspace.content.*;

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
            addViolationDescription("Field has a value", fieldDescription);
            checkedValue = fieldValueList.get(0).value;
            return false;
        }
    }

    protected String getRuleDescription() {
        return String.format("the %s (%s) metadata field %s", fieldDescription, metadataFieldToCheck,
                checkedValue == null ? "is blank" : "has value " + checkedValue);
    }

    public static boolean isBlank(Metadatum dcValue) {
        return dcValue == null || StringUtils.isBlank(dcValue.value);
    }
}
