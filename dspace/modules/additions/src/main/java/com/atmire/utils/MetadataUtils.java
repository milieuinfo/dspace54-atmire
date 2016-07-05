package com.atmire.utils;

import com.atmire.utils.helper.MetadataFieldString;
import com.atmire.utils.subclasses.MetadatumExtended;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.ISO8601DateFormat;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 06 Mar 2014
 */
public class MetadataUtils {

    public static void addAllMetadata(DSpaceObject item, List<Metadatum> values) {
        for (Metadatum value : values) {
            addMetadata(item, value);
        }
    }

    public static void addMetadata(DSpaceObject item, Metadatum dcValue) {
        item.addMetadata(dcValue.schema, dcValue.element, dcValue.qualifier, dcValue.language, dcValue.value, dcValue.authority, dcValue.confidence);
    }

    public static void addMetadata(DSpaceObject item, String mdField, String value) {
        MetadatumExtended metadata = MetadataFieldString.encapsulate(mdField);
        item.addMetadata(metadata.getSchema(), metadata.getElement(), metadata.getQualifier(), metadata.getLanguage(), value);
    }

    public static void clearAllMetadata(DSpaceObject item) {
        item.clearMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
    }

    public static void clearMetadata(DSpaceObject item, Metadatum dcValue) {
        item.clearMetadata(dcValue.schema, dcValue.element, dcValue.qualifier, dcValue.language);
    }

    public static void clearMetadata(DSpaceObject item, String mdField) {
        MetadatumExtended metadata = MetadataFieldString.encapsulate(mdField).withWildcards();
        item.clearMetadata(metadata.getSchema(), metadata.getElement(), metadata.getQualifier(), metadata.getLanguage());
    }

    public static void clearMetadata(DSpaceObject item, String schema, String element, String qualifier, String lang, String authority) {
        // We will build a list of values NOT matching the values to clear
        List<Metadatum> values = new ArrayList<Metadatum>();
        for (Metadatum dcv : getMetadata(item)) {
            if (!match(schema, element, qualifier, lang, authority, dcv)) {
                values.add(dcv);
            }
        }

        // Now swap the old list of values for the new, unremoved values
        clearAllMetadata(item);
        addAllMetadata(item, values);
    }

    public static void copyMetadata(DSpaceObject item, Item item2) {
        List<Metadatum> metadata = getMetadata(item2);
        clearAllMetadata(item);
        addAllMetadata(item, metadata);
    }

    public static List<Metadatum> getMetadata(DSpaceObject item) {
        return getMetadata(item, Item.ANY + "." + Item.ANY + "." + Item.ANY);
    }

    public static List<Metadatum> getMetadata(DSpaceObject item, String mdString) {
        MetadatumExtended elements = MetadataFieldString.encapsulate(mdString).withWildcards();
        Metadatum[] metadata = item.getMetadata(elements.getSchema(), elements.getElement(), elements.getQualifier(), elements.getLanguage());
        return Arrays.asList(metadata);
    }

    public static List<Metadatum> getMetadata(DSpaceObject item, String schema, String element, String qualifier, String lang, String authority) {
        Metadatum[] metadata = item.getMetadata(schema, element, qualifier, lang);
        List<Metadatum> dcValues = Arrays.asList(metadata);
        if (!Item.ANY.equals(authority)) {
            for (Metadatum dcValue : metadata) {
                if (!StringUtils.equals(authority,dcValue.authority)) {
                    dcValues.remove(dcValue);
                }
            }
        }
        return dcValues;
    }

    public static String getMetadataFirstValue(DSpaceObject item, String fieldName) {
        MetadatumExtended elements = MetadataFieldString.encapsulate(fieldName); // this is better not with wildcards
        return getMetadataFirstValue(item, elements.getSchema(), elements.getElement(), elements.getQualifier(), elements.getLanguage());
    }
    public static String getMetadataFirstValueAnyLanguage(DSpaceObject item, String fieldName) {
        MetadatumExtended elements = MetadataFieldString.encapsulate(fieldName);
        return getMetadataFirstValue(item, elements.getSchema(), elements.getElement(), elements.getQualifier(), Item.ANY);
    }

    public static String getMetadataFirstValue(DSpaceObject item, String schema, String element, String qualifier, String language) {
        Metadatum[] metadata = item.getMetadata(schema, element, qualifier, language);
        String value = null;
        if (metadata.length > 0) {
            value = metadata[0].value;
        }
        return value;
    }

    public static void replaceMetadataValue(DSpaceObject item, Metadatum oldValue, Metadatum newValue) {
        // check both dcvalues are for the same field
        if (oldValue.hasSameFieldAs(newValue)) {

            String schema = oldValue.schema;
            String element = oldValue.element;
            String qualifier = oldValue.qualifier;

            // Save all metadata for this field
            Metadatum[] dcvalues = item.getMetadata(schema, element, qualifier, Item.ANY);
            item.clearMetadata(schema, element, qualifier, Item.ANY);

            for (Metadatum dcvalue : dcvalues) {
                if (dcvalue.hasSameFieldAs(oldValue)) {
                    item.addMetadata(schema, element, qualifier, newValue.language, newValue.value, newValue.authority, newValue.confidence);
                } else {
                    item.addMetadata(schema, element, qualifier, dcvalue.language, dcvalue.value, dcvalue.authority, dcvalue.confidence);
                }
            }
        }
    }

    private static boolean match(String schema, String element, String qualifier, String language, String authority, Metadatum dcv) {
        boolean match = match(schema, element, qualifier, language, dcv);
        if (match) {
            if (!authority.equals(Item.ANY)) {
                match = authority.equals(dcv.authority);
            }
        }
        return match;
    }

    /**
     * (yes this is a copy of the original class)
     * <p/>
     * Utility method for pattern-matching metadata elements.  This
     * method will return <code>true</code> if the given schema,
     * element, qualifier and language match the schema, element,
     * qualifier and language of the <code>Metadatum</code> object passed
     * in.  Any or all of the element, qualifier and language passed
     * in can be the <code>Item.ANY</code> wildcard.
     *
     * @param schema    the schema for the metadata field. <em>Must</em> match
     *                  the <code>name</code> of an existing metadata schema.
     * @param element   the element to match, or <code>Item.ANY</code>
     * @param qualifier the qualifier to match, or <code>Item.ANY</code>
     * @param language  the language to match, or <code>Item.ANY</code>
     * @param dcv       the Dublin Core value
     * @return <code>true</code> if there is a match
     */
    private static boolean match(String schema, String element, String qualifier,
                                 String language, Metadatum dcv) {
        // We will attempt to disprove a match - if we can't we have a match
        if (!element.equals(Item.ANY) && !element.equals(dcv.element)) {
            // Elements do not match, no wildcard
            return false;
        }

        if (qualifier == null) {
            // Value must be unqualified
            if (dcv.qualifier != null) {
                // Value is qualified, so no match
                return false;
            }
        } else if (!qualifier.equals(Item.ANY)) {
            // Not a wildcard, so qualifier must match exactly
            if (!qualifier.equals(dcv.qualifier)) {
                return false;
            }
        }

        if (language == null) {
            // Value must be null language to match
            if (dcv.language != null) {
                // Value is qualified, so no match
                return false;
            }
        } else if (!language.equals(Item.ANY)) {
            // Not a wildcard, so language must match exactly
            if (!language.equals(dcv.language)) {
                return false;
            }
        }

        if (!schema.equals(Item.ANY)) {
            if (dcv.schema != null && !dcv.schema.equals(schema)) {
                // The namespace doesn't match
                return false;
            }
        }

        // If we get this far, we have a match
        return true;
    }

    public static String getPII(DSpaceObject item) {
        String piiMdField = ConfigurationManager.getProperty("elsevier-sciencedirect", "metadata.field.pii");
        return MetadataUtils.getMetadataFirstValueAnyLanguage(item, piiMdField);
    }

    public static String getDOI(DSpaceObject item) {
        String doiMdField = ConfigurationManager.getProperty("elsevier-sciencedirect", "metadata.field.doi");
        return MetadataUtils.getMetadataFirstValueAnyLanguage(item, doiMdField);
    }

    public static final String virusCheckDateField = "bitstream.virus.lastScanDate";
    public static final String virusCheckResultField = "bitstream.virus.lastScanResult";

    public static DateFormat getVirusCheckDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss,SSS'Z'");
    }

    public static void updateVirusCheckDateField(Bitstream bitstream) {
        clearMetadata(bitstream, virusCheckDateField);
        String date = getVirusCheckDateFormat().format(new Date());
        addMetadata(bitstream, virusCheckDateField, date);
    }

    public static Date getVirusCheckDate(Bitstream bitstream) {
        String value = getMetadataFirstValue(bitstream, virusCheckDateField);
        if (value != null) {
            try {
                return getVirusCheckDateFormat().parse(value);
            } catch (ParseException e) {
                log.error("bitstream " + bitstream.getID(), e);
            }
        }
        return null;
    }

    private static Logger log = Logger.getLogger(MetadataUtils.class);
}
