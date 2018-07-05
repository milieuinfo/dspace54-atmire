package com.atmire.vocabulary;

import java.util.List;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 08 Jun 2018
 */
public class Field {
    private String field;
    private List<Value> values;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public List<Value> getValues() {
        return values;
    }

    public void setValues(List<Value> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "Field{" +
                "field='" + field + '\'' +
                ", values=" + values +
                '}';
    }
}
