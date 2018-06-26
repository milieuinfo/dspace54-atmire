package com.atmire.sword.rules;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.List;
import java.util.regex.Pattern;

import com.atmire.sword.validation.model.Value;
import org.dspace.content.Metadatum;


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
            addViolationDescription("het %s veld (%s) heeft geen waarde", fieldDescription, metadataFieldToCheck);
        } else {
            for (Metadatum metadatum : fieldValueList) {
                if (!regex.matcher(metadatum.value).matches()) {
                    valid = false;
                    addViolationDescription(
                            "De waarde %s voldoet niet aan de reguliere expressie %s",
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
                "het %s veld (%s) moet voldoen aan het opgelegde formaat",
                fieldDescription,
                metadataFieldToCheck
        );
    }

    protected String getRuleDescriptionViolation() {
        return String.format(
                "het %s veld (%s) heeft niet het opgelegde formaat",
                fieldDescription,
                metadataFieldToCheck
        );
    }
}
