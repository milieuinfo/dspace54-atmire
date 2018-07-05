package com.atmire.sword.rules;

import java.sql.SQLException;
import java.util.List;

import com.atmire.sword.validation.model.Value;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;

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
            addViolationDescription("de opgegeven drempelwaarde %s is geen geldig getal", thresholdValue);
            this.thresholdNumber = null;
        }

    }

    protected String getRuleDescriptionCompliant() {
        return String.format("het aantal waardes van %s (veld %s) is groter dan %s", fieldDescription, fieldToCheck,
                thresholdValue == null ? "ERROR" : getValueDescription(thresholdValue));
    }

    protected String getRuleDescriptionViolation() {
        return String.format("het aantal waardes van %s (veld %s) moet groter zijn dan %s", fieldDescription, fieldToCheck,
                thresholdValue == null ? "ERROR" : getValueDescription(thresholdValue));
    }

    protected boolean doValidationAndBuildDescription(final Context context, final Item item) {
        boolean valid = false;

        if (fieldToCheck == null) {
            addViolationDescription("een leeg veld kan niet gevalideerd worden");
        } else {
            if(thresholdNumber != null) {
                try {
                    int count = countFieldValues(context, item);

                    if (count > thresholdNumber) {
                        valid = true;
                    } else {
                        addViolationDescription("het aantal waardes van %s is %d", fieldDescription, count);
                    }

                } catch (SQLException e) {
                    addViolationDescription("het is niet mogelijk om de waardes van %s te tellen: %s", fieldDescription, e.getMessage());
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
