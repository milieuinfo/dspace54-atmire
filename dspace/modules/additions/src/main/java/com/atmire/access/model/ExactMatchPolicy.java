package com.atmire.access.model;


import javax.xml.bind.annotation.*;
import org.apache.commons.lang3.*;
import org.apache.log4j.*;
import org.dspace.content.*;
import org.dspace.eperson.*;

/**
 * @author philip at atmire.com
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "exact-match-policy")
public class ExactMatchPolicy implements Policy {

    private static Logger log = Logger.getLogger(ExactMatchPolicy.class);

    @XmlElement(name="itemField")
    private ItemField itemField;

    @XmlElement(name="epersonField")
    private EpersonField epersonField;

    public ItemField getItemField() {
        return itemField;
    }

    public void setItemField(ItemField itemField) {
        this.itemField = itemField;
    }

    public EpersonField getEpersonField() {
        return epersonField;
    }

    public void setEpersonField(EpersonField epersonField) {
        this.epersonField = epersonField;
    }

    @Override
    public boolean isAuthorized(EPerson ePerson, Item item) {
        log.debug("ExactMatchPolicy.isAuthorized");
        log.debug("ePerson: " + ePerson.getEmail());

        Metadatum[] ePersonMetadata = getEpersonMetadata(ePerson);
        Metadatum[] itemMetadata = getItemMetadata(item);

        boolean printedDebug = false;
        for (Metadatum itemMetadatum : itemMetadata) {
            log.debug("item field " + itemMetadatum.getField() + " with value " + itemMetadatum.value);
            for (Metadatum ePersonMetadatum : ePersonMetadata) {

                if(!printedDebug) {
                    log.debug("eperson field " + ePersonMetadatum.getField() + " with value " + ePersonMetadatum.value);
                }

                if(itemMetadatum.value.equals(ePersonMetadatum.value))
                {
                    return true;
                }
            }

            printedDebug = true;
        }

        return false;
    }

    private Metadatum[] getEpersonMetadata(EPerson ePerson) {
        String[] split = StringUtils.split(epersonField.getValue(), ".");

        String schema = split[0];
        String element = split[1];
        String qualifier = null;

        if(split.length > 2) {
            qualifier = split[2];
        }

        return ePerson.getMetadata(schema, element, qualifier, Item.ANY);
    }

    private Metadatum[] getItemMetadata(Item item) {
        String[] split = StringUtils.split(itemField.getValue(), ".");

        String schema = split[0];
        String element = split[1];
        String qualifier = null;

        if(split.length > 2) {
            qualifier = split[2];
        }

        return item.getMetadata(schema, element, qualifier, Item.ANY);
    }
}
