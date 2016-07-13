package com.atmire.access.model;


import javax.xml.bind.annotation.*;

/**
 * @author philip at atmire.com
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "exact-match-policy")
public class ExactMatchPolicy {

    @XmlElement(name="itemField")
    private ItemField itemField;

    @XmlElement(name="epersonField")
    private EpersonField epersonField;

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
}
