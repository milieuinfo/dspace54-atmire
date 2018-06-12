package com.atmire.sword.rules;

import com.atmire.sword.validation.model.Value;
import org.dspace.content.Metadatum;

import java.util.List;
import java.util.regex.Pattern;

import static org.apache.commons.collections.CollectionUtils.isEmpty;


/**
 * Rule to check if a date value of a given metadata field is smaller than a specified value
 */
public class RegexRule extends AbstractFieldCheckRule implements ComplianceRule {

    private final Pattern regex;

    public RegexRule(
            final String fieldDescription,
            final String metadataField,
            List<Value> fieldValue
    ) {
        super(fieldDescription, metadataField);
        this.regex = Pattern.compile(fieldValue.get(0).getValue());
    }

    protected boolean checkFieldValues(final List<Metadatum> fieldValueList) {
        boolean valid = true;
        if (isEmpty(fieldValueList)) {
            valid = false;
            addViolationDescription("The %s field has no value", fieldDescription);
        } else {
            for (Metadatum metadatum : fieldValueList) {
                if (!regex.matcher(metadatum.value).matches()) {
                    valid = false;
                    addViolationDescription(
                            "The value %s does not satisfy the regex %s",
                            metadatum.value,
                            regex.pattern()
                    );
                }
            }
        }
        return valid;
    }

    protected String getRuleDescriptionCompliant() {
        return String.format(
                "The %s (%s) field has the expected format",
                fieldDescription,
                metadataFieldToCheck
        );
    }

    protected String getRuleDescriptionViolation() {
        return String.format(
                "The %s (%s) field does not have the expected format",
                fieldDescription,
                metadataFieldToCheck
        );
    }
}
