package com.atmire.utils.subclasses;

import com.atmire.utils.helper.MetadataFieldString;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;

/**
* Created by: Antoine Snyers (antoine at atmire dot com)
* Date: 19 Sep 2014
*/
public class MetadatumExtended extends Metadatum {

    /**
    * @param metadataFieldString schema.element.qualifier[language]::authority::confidence
    */
    public MetadatumExtended(String metadataFieldString, String value) {
        this(MetadataFieldString.encapsulate(metadataFieldString), value);
    }

    public MetadatumExtended(Metadatum field, String value){
        this(field.schema, field.element, field.qualifier, field.language, value, field.authority, field.confidence);
    }

    public MetadatumExtended(Metadatum dcValue) {
        this(dcValue.schema, dcValue.element, dcValue.qualifier, dcValue.language, dcValue.value, dcValue.authority, dcValue.confidence);
    }

    public MetadatumExtended(String schema, String element, String qualifier, String language, String value, String authority, int confidence) {
        this.schema = schema;
        this.element = element;
        this.qualifier = qualifier;
        this.language = language;
        this.value = value;
        this.authority = authority;
        this.confidence = confidence;
    }

    public MetadatumExtended() {

    }

    public String getSchema() {
        return schema;
    }


    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

    public MetadatumExtended withWildcards() {
        String wilcard = Item.ANY;
        String schema1 = StringUtils.isBlank(schema) ? wilcard : schema;
        String element1 = StringUtils.isBlank(element) ? wilcard : element;
        String qualifier1 = StringUtils.isBlank(qualifier) ? wilcard : qualifier;
        String language1 = StringUtils.isBlank(language) ? wilcard : language;
        return new MetadatumExtended(schema1, element1, qualifier1, language1, value, authority, confidence);
    }
}
