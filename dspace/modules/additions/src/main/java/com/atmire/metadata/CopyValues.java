package com.atmire.metadata;

import com.atmire.utils.MetadataUtils;
import org.dspace.content.Metadatum;

import java.util.List;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 07 Jun 2018
 */
public class CopyValues extends AbstractFillValue {

    private String sourceField;

    public String getSourceField() {
        return sourceField;
    }

    public void setSourceField(String sourceField) {
        this.sourceField = sourceField;
    }

    @Override
    public List<Metadatum> getValues(EditParameters parameters) {
        List<Metadatum> metadata
                = MetadataUtils.getMetadata(parameters.getObject(), getSourceField());
        return convertField(metadata);
    }

}
