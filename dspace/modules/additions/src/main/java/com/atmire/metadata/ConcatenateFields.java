package com.atmire.metadata;

import com.atmire.utils.MetadataUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Metadatum;

import java.util.Iterator;
import java.util.List;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 07 Jun 2018
 */
public class ConcatenateFields extends AbstractFillValue {

    private List<String> fields;
    private String separator;

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    @Override
    public List<Metadatum> getValues(EditParameters parameters) {
        String value = null;
        DSpaceObject object = parameters.getObject();
        Iterator<String> iterator = fields.iterator();
        boolean useNextField = iterator.hasNext();
        while (useNextField) {
            String nextField = iterator.next();
            String nextValue = MetadataUtils.getMetadataFirstValueAnyLanguage(object, nextField);
            if (StringUtils.isNotBlank(nextValue)) {
                if (value == null) {
                    value = nextValue;
                } else {
                    //noinspection StringConcatenationInLoop
                    value += getSeparator() + nextValue;
                }
                useNextField = iterator.hasNext();
            } else {
                useNextField = false;
            }
        }
        return convert(emptyIfNull(value));
    }
}
