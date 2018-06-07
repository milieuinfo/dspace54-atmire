package com.atmire.metadata;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 07 Jun 2018
 */
public class FillValue {

    private String field;
    private String value;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getValue(FillValueDependencies parameters) {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
