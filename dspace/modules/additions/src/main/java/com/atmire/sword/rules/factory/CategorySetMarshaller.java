package com.atmire.sword.rules.factory;


import com.atmire.sword.validation.model.*;
import com.atmire.utils.*;

/**
 * XML marshaller to unmarshall or marshall the validation rule definition file
 */
public class CategorySetMarshaller extends XmlMarshaller<CategorySet> {

    public CategorySetMarshaller() {
        super(CategorySet.class);
    }

}
