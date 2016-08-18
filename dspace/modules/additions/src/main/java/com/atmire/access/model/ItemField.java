package com.atmire.access.model;

import javax.xml.bind.annotation.*;

/**
 * @author philip at atmire.com
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "itemField")
public class ItemField {

    @XmlValue
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
