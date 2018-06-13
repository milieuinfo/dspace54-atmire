package com.atmire.sword.rules;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.dspace.content.Metadatum;


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
            addViolationDescription("het %s veld heeft geen waarde", fieldDescription);
        } else {
            try {
                Date thresholdDate = parseDate(getThresholdValue());
                Date dateToCheck = parseDate(fieldValueList.get(0).value);

                if (dateToCheck == null) {
                    addViolationDescription("er is geen geldige waarde voor het veld " + metadataFieldToCheck);
                } else if (thresholdDate != null && dateToCheck.compareTo(thresholdDate) < 0) {
                    valid = true;
                } else {
                    addViolationDescription("de %s is na vandaag", fieldDescription);
                }

            } catch (IllegalArgumentException ex) {
                addViolationDescription(
                        "het veld %s (%s) is ongeldig omdat het een ongeldige datum (-formaat) bevat",
                        fieldDescription, metadataFieldToCheck
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
                "het %s veld (%s) ligt in het verleden of op vandaag",
                fieldDescription,
                metadataFieldToCheck
        );
    }

    protected String getRuleDescriptionViolation() {
        return String.format(
                "het %s veld (%s) moet in het verleden of vandaag zijn",
                fieldDescription,
                metadataFieldToCheck
        );
    }
}
