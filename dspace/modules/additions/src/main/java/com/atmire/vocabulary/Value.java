package com.atmire.vocabulary;

import java.util.List;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 08 Jun 2018
 */
public class Value {
    private String value;
    private List<Field> fields;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    @Override
    public String toString() {
        return "Value{" +
                "value='" + value + '\'' +
                ", fields=" + fields +
                '}';
    }
}
