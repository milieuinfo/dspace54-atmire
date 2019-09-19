package com.atmire.sword.rules;

import java.util.List;

import com.atmire.sword.validation.model.Value;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.joda.time.DateTime;
import org.joda.time.Months;

/**
 * Rule to check if a date range defined by two metadata fields is smaller than a specified threshold
 */
public class DateRangeSmallerThanRule extends AbstractComplianceRule {

    private String fromField;
    private String toField;
    private String rangeDescription;

    private Value thresholdValue;
    private Integer thresholdNumber;

    public DateRangeSmallerThanRule(final String fromField, final String toField, final String rangeDescription,
                                    final List<Value> thresholdValues) {
        this.fromField = StringUtils.trimToNull(fromField);
        this.toField = StringUtils.trimToNull(toField);
        this.rangeDescription = StringUtils.trimToNull(rangeDescription);

        thresholdValue = CollectionUtils.isEmpty(thresholdValues) ? null : thresholdValues.get(0);

        try {
            this.thresholdNumber = thresholdValue == null ? null : Integer.valueOf(StringUtils.trimToEmpty(thresholdValue.getValue()));
        } catch(NumberFormatException ex) {
            addViolationDescription("de opgegeven drempelwaarde %s is geen geldig getal", thresholdValue);
            this.thresholdNumber = null;
        }
    }

    protected String getRuleDescriptionCompliant() {
        return String.format("de %s (van %s tot %s) is minder dan %s maand(en)", rangeDescription, fromField, toField,
                thresholdValue == null ? "ERROR" : getValueDescription(thresholdValue));
    }

    protected String getRuleDescriptionViolation() {
        return String.format("de %s (van %s tot %s) moet minder zijn dan %s maand(en)", rangeDescription, fromField, toField,
                thresholdValue == null ? "ERROR" : getValueDescription(thresholdValue));
    }

    protected boolean doValidationAndBuildDescription(final Context context, final Item item) {
        boolean valid = false;

        if (fromField == null || toField == null) {
            addViolationDescription("the from and to date fields of a date range validation rule cannot be blank.");
        } else {
            if(thresholdNumber != null) {
                DateTime from = getFirstDateValue(context, item, fromField);
                DateTime to = getFirstDateValue(context, item, toField);

                if(from == null) {
                    addViolationDescription("er is geen geldige waarde voor het veld " + fromField);
                }
                if(to == null) {
                    addViolationDescription("er is geen geldige waarde voor het veld " + toField);
                }

                if (to != null && from != null) {
                    if(to.isAfter(from)){
                        to = to.minusDays(1);
                    }
                    int months = Months.monthsBetween(from.toLocalDate(), to.toLocalDate()).getMonths();

                    if (months < thresholdNumber) {
                        valid = true;
                    } else {
                        addViolationDescription("de %s is %d maand(en)", rangeDescription, months);
                    }
                }
            }
        }

        return valid;
    }

}
