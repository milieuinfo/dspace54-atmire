package com.atmire.access.model;


import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.LinkedList;
import java.util.List;

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

    @XmlElement(name="epersonValueExtractor")
    private EpersonValueExtractor epersonValueExtractor;

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

    public EpersonValueExtractor getEpersonValueExtractor() {
        return epersonValueExtractor;
    }

    public void setEpersonValueExtractor(EpersonValueExtractor epersonValueExtractor) {
        this.epersonValueExtractor = epersonValueExtractor;
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

                String epersonAclValue = epersonValueExtractor.extractEpersonAclValue(ePersonMetadatum.value);
                if(itemMetadatum.value.equals(epersonAclValue))
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

    public String getSolrIndexField(){
        return itemField.getValue()+"_keyword";
    }

    public List<String> getSolrIndexValues(Context context, DSpaceObject dSpaceObject) {
        List<String> output = null;

        if (dSpaceObject != null) {
            output = new LinkedList<>();
            String[] elements = DSpaceObject.getElementsFilled(itemField.getValue());
            List<Metadatum> metadata = dSpaceObject.getMetadata(elements[0], elements[1], elements[2], elements[3], Item.ANY);
            for(Metadatum value : metadata) {
                output.add(value.value);
            }
        }

        return output;
    }

    public String getSolrQueryCriteria(EPerson ePerson){

        Metadatum[] metadata = getEpersonMetadata(ePerson);
        StringBuilder solrQueryCriteria = new StringBuilder();

        solrQueryCriteria.append("(");
        if(ArrayUtils.isNotEmpty(metadata)) {

            for (Metadatum metadatum : metadata) {
                String epersonAclValue = epersonValueExtractor.extractEpersonAclValue(metadatum.value);

                if(StringUtils.isNotBlank(epersonAclValue)) {
                    if (solrQueryCriteria.length() > 1) {
                        solrQueryCriteria.append(" OR ");
                    }

                    solrQueryCriteria.append(getSolrIndexField() + ":\"" + epersonAclValue + "\"");
                }
            }
        } else {
            solrQueryCriteria.append(getSolrIndexField() + ":\"NO-VALUE-FOUND\"");
        }

        solrQueryCriteria.append(")");

        return solrQueryCriteria.toString();
    }
}
