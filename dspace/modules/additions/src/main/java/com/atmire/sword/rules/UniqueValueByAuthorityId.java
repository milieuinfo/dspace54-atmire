package com.atmire.sword.rules;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;

import java.util.List;

/**
 * @author philip at atmire.com
 */
public class UniqueValueByAuthorityId extends AbstractFieldCheckRule {

    public boolean valid;

    public UniqueValueByAuthorityId(String fieldDescription, String metadataField) {
        super(fieldDescription, metadataField);
    }

    @Override
    protected boolean checkFieldValues(List<Metadatum> fieldValueList) {
        valid = true;
        for (Metadatum metadatum : fieldValueList) {
            Context context = null;
            try {
                context = new Context();
                context.turnOffAuthorisationSystem();

                // Reverted back to using value -> Authority isn't filled in at this point, the consumer that copies this value to the authority is only triggered after this step.
                if(StringUtils.isNotBlank(metadatum.value)) {
                    String[] split = StringUtils.split(metadataFieldToCheck, ".");

                    if(split.length>=2) {
                        String schema = split[0];
                        String element = split[1];
                        String qualifier = null;

                        if(split.length == 3){
                            qualifier = split[2];
                        }

                        ItemIterator itemIterator = Item.findByAuthorityValue(context, schema, element, qualifier, metadatum.value);

                        if (itemIterator.hasNext()) {
                            valid = false;
                            addViolationDescription("The value for %s is already used by item %s", fieldDescription, itemIterator.next().getHandle());
                        }
                    }
                }
            } catch (Exception e) {
                valid = false;
                addViolationDescription("unable to check unique value for field %s", fieldDescription);
            }
            finally {
                if(context!= null && context.isValid()){
                    context.abort();
                }
            }
        }

        return valid;
    }

    @Override
    protected String getRuleDescriptionCompliant() {
        return String.format("the %s (%s) metadata field is unique",
                             fieldDescription,
                             metadataFieldToCheck
        );
    }

    @Override
    protected String getRuleDescriptionViolation() {
        return String.format("the %s (%s) metadata field is not unique",
                             fieldDescription,
                             metadataFieldToCheck
        );
    }
}
