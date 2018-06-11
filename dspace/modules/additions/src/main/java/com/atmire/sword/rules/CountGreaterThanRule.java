package com.atmire.sword.rules;

import com.atmire.sword.validation.model.*;
import java.sql.*;
import java.util.*;
import org.apache.commons.collections.*;
import org.apache.commons.lang.*;
import org.dspace.content.*;
import org.dspace.core.*;

/**
 * Rule to check if an item has more than X values for a specified field.
 */
public class CountGreaterThanRule extends AbstractComplianceRule {

    private String fieldToCheck;
    private String fieldDescription;

    private Value thresholdValue;
    private Integer thresholdNumber;

    public CountGreaterThanRule(final String fieldDescription, final String fieldToCheck, final List<Value> thresholdValues) {
        this.fieldDescription = StringUtils.trimToEmpty(fieldDescription);
        this.fieldToCheck = StringUtils.trimToNull(fieldToCheck);

        thresholdValue = CollectionUtils.isEmpty(thresholdValues) ? null : thresholdValues.get(0);

        try {
            this.thresholdNumber = thresholdValue == null ? null : Integer.valueOf(StringUtils.trimToEmpty(thresholdValue.getValue()));
        } catch(NumberFormatException ex) {
            addViolationDescription("the provided threshold value %s is not a number", thresholdValue);
            this.thresholdNumber = null;
        }

    }

    protected String getRuleDescriptionCompliant() {
        return String.format("the number of %s (field %s) is greater than %s", fieldDescription, fieldToCheck,
                thresholdValue == null ? "ERROR" : getValueDescription(thresholdValue));
    }

    protected String getRuleDescriptionViolation() {
        return String.format("the number of %s (field %s) must be greater than %s", fieldDescription, fieldToCheck,
                thresholdValue == null ? "ERROR" : getValueDescription(thresholdValue));
    }

    protected boolean doValidationAndBuildDescription(final Context context, final Item item) {
        boolean valid = false;

        if (fieldToCheck == null) {
            addViolationDescription("cannot validate a blank field");
        } else {
            if(thresholdNumber != null) {
                try {
                    int count = countFieldValues(context, item);

                    if (count > thresholdNumber) {
                        valid = true;
                    } else {
                        addViolationDescription("the number of %s is %d", fieldDescription, count);
                    }

                } catch (SQLException e) {
                    addViolationDescription("unable to count values for field %s: %s", fieldDescription, e.getMessage());
                }
            }
        }
        return valid;
    }

    private int countFieldValues(final Context context, final Item item) throws SQLException {
        List<Metadatum> fieldValueList = getMetadata(context, item, fieldToCheck);
        return CollectionUtils.size(fieldValueList);
    }

}
