package com.atmire.sword.rules;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.atmire.sword.validation.model.Value;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.content.Metadatum;

/**
 * Rule that will check if an item field has a specified value
 */
public class FieldHasValueRule extends AbstractFieldCheckRule {

    private Map<String, Value> possibleValues = new LinkedHashMap<String, Value>();

    private Value checkedCompliantValue = null;

    public FieldHasValueRule(final String fieldDescription, final String metadataField, final Collection<Value> possibleValues) {
        super(fieldDescription, metadataField);

        if(CollectionUtils.isNotEmpty(possibleValues)) {
            for (Value possibleValue : possibleValues) {
                this.possibleValues.put(StringUtils.trimToEmpty(possibleValue.getValue()), possibleValue);
            }
        }
    }

    protected boolean checkFieldValues(final List<Metadatum> fieldValueList) {

        if (isEmpty(fieldValueList)) {
            addViolationDescription("Het %s veld heeft geen waarde", fieldDescription);
            return false;
        } else if(possibleValues.containsKey(fieldValueList.get(0).value)) {
            checkedCompliantValue = possibleValues.get(fieldValueList.get(0).value);
            return true;
        } else {
            addViolationDescription("het %s veld heeft waarde %s", fieldDescription, fieldValueList.get(0).value);
            return false;
        }
    }

    protected String getRuleDescriptionCompliant() {
        return String.format("het %s veld (%s) heeft waarde %s", fieldDescription, metadataFieldToCheck,
                checkedCompliantValue == null ? buildValueString() : getValueDescription(checkedCompliantValue));
    }

    protected String getRuleDescriptionViolation() {
        return String.format("het %s veld (%s) moet waarde %s hebben", fieldDescription, metadataFieldToCheck,
                checkedCompliantValue == null ? buildValueString() : getValueDescription(checkedCompliantValue));
    }

    private String buildValueString() {
        if(possibleValues.size() == 1) {
            return getValueDescription(possibleValues.values().iterator().next());

        } else if(possibleValues.size() > 1) {
            StringBuilder output = new StringBuilder();
            int i = 1;

            for (Value value : possibleValues.values()) {
                output.append(getValueDescription(value));
                if(i == (possibleValues.size() - 1)) {
                    output.append(" or ");
                } else if(i < possibleValues.size()){
                    output.append(", ");
                }
                i++;
            }

            return output.toString();
        } else {
            return "";
        }
    }
}
