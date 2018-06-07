package com.atmire.metadata;

import org.dspace.content.Metadatum;

import java.util.Collections;
import java.util.List;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 07 Jun 2018
 */
public class FillValue extends AbstractFillValue {

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public List<Metadatum> getValues(EditParameters parameters) {
        return convert(Collections.singletonList(getValue()));
    }

}
