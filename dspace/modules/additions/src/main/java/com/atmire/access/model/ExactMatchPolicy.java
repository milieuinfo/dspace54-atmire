package com.atmire.access.model;


import java.util.*;
import java.util.regex.*;
import javax.xml.bind.annotation.*;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.*;
import org.dspace.content.*;
import org.dspace.core.*;
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

    @XmlElement(name="epersonValueMatcher")
    private EpersonValueMatcher epersonValueMatcher;

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

    public EpersonValueMatcher getEpersonValueMatcher() {
        return epersonValueMatcher;
    }

    public void setEpersonValueMatcher(EpersonValueMatcher epersonValueMatcher) {
        this.epersonValueMatcher = epersonValueMatcher;
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

                if(itemMetadatum.value.equals(getEpersonMatchedValue(ePersonMetadatum.value)))
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

    protected String getEpersonMatchedValue(String value) {
        String matchedValue = null;

        Pattern patt = Pattern.compile(epersonValueMatcher.getValue());
        Matcher matcher = patt.matcher(value);
        if (matcher.find()) {
            matchedValue = matcher.group(1);
        }

        return matchedValue;
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

        if(ArrayUtils.isNotEmpty(metadata)) {
            solrQueryCriteria.append("(");

            for (Metadatum metadatum : metadata) {
                String epersonMatchedValue = getEpersonMatchedValue(metadatum.value);

                if(StringUtils.isNotBlank(epersonMatchedValue)) {
                    if (solrQueryCriteria.length() > 1) {
                        solrQueryCriteria.append(" OR ");
                    }

                    solrQueryCriteria.append(getSolrIndexField() + ":" + epersonMatchedValue);
                }
            }

            solrQueryCriteria.append(")");
        }

        return solrQueryCriteria.toString();
    }
}
