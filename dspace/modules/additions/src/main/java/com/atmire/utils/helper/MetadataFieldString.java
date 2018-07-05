package com.atmire.utils.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.atmire.utils.subclasses.MetadatumExtended;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.util.DCInput;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.Choices;

/**
 * metadataFieldString = schema.element.qualifier[language]::authority::confidence
 * qualifier is optional
 * [language] is optional
 * authority is optional
 * confidence: not if no authority, otherwise optional
 * <p/>
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 19 Sep 2014
 */
public class MetadataFieldString {

    private enum Parts {
        SCHEMA, ELEMENT, QUALIFIER, LANGUAGE, AUTHORITY, CONFIDENCE
    }

    private static final String SIGNATURE_SEPARATOR = ".";
    private static final String AUTHORITY_SEPARATOR = "::";

    public static String getSchema(String metadataFieldString) {
        return parse(metadataFieldString).get(Parts.SCHEMA.ordinal());
    }

    public static String getElement(String metadataFieldString) {
        return parse(metadataFieldString).get(Parts.ELEMENT.ordinal());
    }

    public static String getQualifier(String metadataFieldString) {
        return parse(metadataFieldString).get(Parts.QUALIFIER.ordinal());
    }

    public static String getLanguage(String metadataFieldString) {
        return parse(metadataFieldString).get(Parts.LANGUAGE.ordinal());
    }

    public static String getAuthority(String metadataFieldString) {
        return parse(metadataFieldString).get(Parts.AUTHORITY.ordinal());
    }

    public static int getConfidence(String metadataFieldString) {
        int confidence;
        try {
            confidence = Integer.parseInt(parse(metadataFieldString).get(Parts.CONFIDENCE.ordinal()));
        } catch (NumberFormatException n) {
            confidence = Choices.CF_UNSET;
        }
        return confidence;
    }

    private static List<String> parse(String metadataFieldString) {
        String schema, element, qualifier = null, language = null, authority = null, confidence = null;

        String[] tokens;
        if (StringUtils.isBlank(metadataFieldString)) {
            tokens = new String[]{};
        } else {
            tokens = StringUtils.split(metadataFieldString, SIGNATURE_SEPARATOR);
        }

        if (tokens.length < 2) {
            throw new IllegalArgumentException("invalid metadataFieldString: too few tokens");
        } else if (tokens.length >= 4) {
            throw new IllegalArgumentException("invalid metadataFieldString: too many tokens");
        } else {
            schema = tokens[0];
            element = tokens[1];

            if (tokens.length == 3) {
                String rest = tokens[2];

                String[] authoritySplit = StringUtils.split(rest, AUTHORITY_SEPARATOR);
                if (authoritySplit.length > 1) {
                    authority = authoritySplit[1];
                }
                if (authoritySplit.length > 2) {
                    confidence = authoritySplit[2];
                }

                rest = authoritySplit[0];
                if (rest.contains("[")) {
                    int open = rest.indexOf('[');
                    int close = rest.indexOf(']');
                    qualifier = rest.substring(0, open);
                    language = rest.substring(open + 1, close);
                } else {
                    qualifier = rest;
                }
            } else if(element.contains("[")) {
                int open = element.indexOf('[');
                int close = element.indexOf(']');
                language = element.substring(open + 1, close);
                element = element.substring(0, open);
            }
        }

        ArrayList<String> parsed = new ArrayList<String>();
        Collections.addAll(parsed, new String[Parts.values().length]);
        parsed.set(Parts.SCHEMA.ordinal(), schema);
        parsed.set(Parts.ELEMENT.ordinal(), element);
        parsed.set(Parts.QUALIFIER.ordinal(), qualifier);
        parsed.set(Parts.LANGUAGE.ordinal(), language);
        parsed.set(Parts.AUTHORITY.ordinal(), authority);
        parsed.set(Parts.CONFIDENCE.ordinal(), confidence);

        return parsed;
    }

    public static String representing(Metadatum dcValue) {
        StringBuilder builder = new StringBuilder();
        builder.append(StringUtils.isNotBlank(dcValue.schema) ? dcValue.schema : Item.ANY)
                .append(SIGNATURE_SEPARATOR)
                .append(StringUtils.isNotBlank(dcValue.element) ? dcValue.element : Item.ANY);
        if (StringUtils.isNotBlank(dcValue.qualifier)) {
            builder.append(SIGNATURE_SEPARATOR).append(dcValue.qualifier);
        }
        if (StringUtils.isNotBlank(dcValue.language)) {
            builder.append('[').append(dcValue.language).append(']');
        }
        if (StringUtils.isNotBlank(dcValue.authority)) {
            builder.append(AUTHORITY_SEPARATOR).append(dcValue.authority);
            builder.append(AUTHORITY_SEPARATOR).append(dcValue.confidence);
        }
        return builder.toString();
    }

    public static String representing(DCInput input) {
        StringBuilder builder = new StringBuilder();
        builder.append(StringUtils.isNotBlank(input.getSchema()) ? input.getSchema() : Item.ANY)
                .append(SIGNATURE_SEPARATOR)
                .append(StringUtils.isNotBlank(input.getElement()) ? input.getElement() : Item.ANY);
        if (StringUtils.isNotBlank(input.getQualifier())) {
            builder.append(SIGNATURE_SEPARATOR).append(input.getQualifier());
        }
        return builder.toString();
    }

    public static MetadatumExtended encapsulate(String metadataFieldString, boolean noSchema) {
        if (noSchema) {
            metadataFieldString = "bogus" + SIGNATURE_SEPARATOR + metadataFieldString;
        }
        return encapsulate(metadataFieldString);
    }

    public static MetadatumExtended encapsulate(String metadataFieldString) {
        String schema = getSchema(metadataFieldString);
        String element = getElement(metadataFieldString);
        String qualifier = getQualifier(metadataFieldString);
        String language = getLanguage(metadataFieldString);
        String authority = getAuthority(metadataFieldString);
        int confidence = getConfidence(metadataFieldString);
        return new MetadatumExtended(schema, element, qualifier, language, null, authority, confidence);
    }
}
