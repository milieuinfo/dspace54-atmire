package com.atmire.sword.rules;

import org.dspace.content.Metadatum;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.apache.commons.collections.CollectionUtils.isEmpty;


/**
 * Rule to check if a date value of a given metadata field is smaller than a specified value
 */
public class PastOrCurrentDateRule extends AbstractFieldCheckRule implements ComplianceRule {

    public PastOrCurrentDateRule(final String fieldDescription, final String metadataField) {
        super(fieldDescription, metadataField);
    }

    protected boolean checkFieldValues(final List<Metadatum> fieldValueList) {
        boolean valid = false;
        if (isEmpty(fieldValueList)) {
            addViolationDescription("The %s field has no value", fieldDescription);
        } else {
            try {
                Date thresholdDate = parseDate(getThresholdValue());
                Date dateToCheck = parseDate(fieldValueList.get(0).value);

                if (dateToCheck == null) {
                    addViolationDescription("there is no valid value for the field " + metadataFieldToCheck);
                } else if (thresholdDate != null && dateToCheck.compareTo(thresholdDate) < 0) {
                    valid = true;
                } else {
                    addViolationDescription("the %s is after today", fieldDescription);
                }

            } catch (IllegalArgumentException ex) {
                addViolationDescription(
                        "the metadata field %s is invalid because it has too few tokens or contains an invalid date",
                        metadataFieldToCheck
                );
            }
        }

        return valid;
    }

    protected Date parseDate(String inputString) throws IllegalArgumentException {
        try {
            return getDateFormat().parse(inputString);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String getThresholdValue() {
        return getDateFormat().format(new Date());
    }

    private DateFormat getDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd");
    }

    protected String getRuleDescriptionCompliant() {
        return String.format(
                "the %s (%s) is today or before today",
                fieldDescription,
                metadataFieldToCheck
        );
    }

    protected String getRuleDescriptionViolation() {
        return String.format(
                "the %s (%s) must be today or before today",
                fieldDescription,
                metadataFieldToCheck
        );
    }
}
