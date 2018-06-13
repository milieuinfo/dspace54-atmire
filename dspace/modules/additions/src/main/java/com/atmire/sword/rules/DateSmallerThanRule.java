package com.atmire.sword.rules;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.List;

import com.atmire.sword.validation.model.Value;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Metadatum;
import org.joda.time.DateTime;


/**
 * Rule to check if a date value of a given metadata field is smaller than a specified value
 */
public class DateSmallerThanRule extends AbstractFieldCheckRule implements ComplianceRule {

    private Value thresholdValue;

    public DateSmallerThanRule(final String fieldDescription, final String metadataField, final List<Value> thresholdValues) {
        super(fieldDescription, metadataField);

        thresholdValue = CollectionUtils.isEmpty(thresholdValues) ? null : thresholdValues.get(0);
    }

    protected boolean checkFieldValues(final List<Metadatum> fieldValueList) {
        boolean valid = false;
        if (isEmpty(fieldValueList)) {
            addViolationDescription("het %s veld heeft geen waarde", fieldDescription);

        } else if (thresholdValue == null || StringUtils.isBlank(thresholdValue.getValue())) {
            addViolationDescription("de drempelwaarde kan niet leeg zijn");

        } else {
            try {
                DateTime thresholdDate = parseDateTime(thresholdValue.getValue());
                DateTime dateToCheck = parseDateTime(fieldValueList.get(0).value);

                if(dateToCheck == null) {
                    addViolationDescription("er is geen geldige waarde voor het veld " + metadataFieldToCheck);
                } else if (thresholdDate != null && dateToCheck.compareTo(thresholdDate) < 0) {
                    valid = true;
                } else {
                    addViolationDescription("de %s is na %s", fieldDescription,
                            thresholdValue == null ? "ERROR" : getValueDescription(thresholdValue));
                }

            } catch (IllegalArgumentException ex) {
                addViolationDescription("het metadata veld %s is ongeldig omdat het een ongeldige datum (-formaat) bevat", metadataFieldToCheck);
            }
        }

        return valid;
    }

    protected String getRuleDescriptionCompliant() {
        return String.format("de %s (%s) is voor %s", fieldDescription, metadataFieldToCheck,
                thresholdValue == null ? "ERROR" : getValueDescription(thresholdValue));
    }

    protected String getRuleDescriptionViolation() {
        return String.format("de %s (%s) moest voor %s zijn", fieldDescription, metadataFieldToCheck,
                thresholdValue == null ? "ERROR" : getValueDescription(thresholdValue));
    }
}
