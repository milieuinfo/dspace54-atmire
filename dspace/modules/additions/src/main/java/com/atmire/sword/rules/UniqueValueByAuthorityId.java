package com.atmire.sword.rules;

import java.util.*;
import org.apache.commons.lang.*;
import org.dspace.content.*;
import org.dspace.core.*;

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

                if(StringUtils.isNotBlank(metadatum.value)) {
                    String[] split = StringUtils.split(metadataFieldToCheck, ".");

                    if(split.length>=2) {
                        String schema = split[0];
                        String element = split[1];
                        String qualifier = null;

                        if(split.length == 3){
                            qualifier = split[3];
                        }

                        ItemIterator itemIterator = Item.findByAuthorityValue(context, schema, element, qualifier, metadatum.value);

                        if (itemIterator.hasNext()) {
                            valid = false;
                            addViolationDescription("%s value is already used by item %s", fieldDescription, itemIterator.next().getHandle());
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
    protected String getRuleDescription() {
        return String.format("the %s (%s) metadata field %s", fieldDescription, metadataFieldToCheck,
                valid? "is unique" : "is not unique ");
    }
}
