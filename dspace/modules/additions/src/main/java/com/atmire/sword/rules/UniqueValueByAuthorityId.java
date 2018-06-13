package com.atmire.sword.rules;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;

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
                            addViolationDescription("De waarde voor %s (%s) is reeds in gebruik door item %s", fieldDescription, metadataFieldToCheck,
                                    itemIterator.next().getHandle());
                        }
                    }
                }
            } catch (Exception e) {
                valid = false;
                addViolationDescription("niet in staat om de uniekheid van veld %s te bepalen", fieldDescription);
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
        return String.format("het %s veld (%s) heeft een unieke waarde",
                             fieldDescription,
                             metadataFieldToCheck
        );
    }

    @Override
    protected String getRuleDescriptionViolation() {
        return String.format("het %s veld (%s) moet een unieke waarde hebben",
                             fieldDescription,
                             metadataFieldToCheck
        );
    }
}
