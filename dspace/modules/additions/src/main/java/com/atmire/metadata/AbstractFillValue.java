package com.atmire.metadata;

import com.atmire.utils.MetadataUtils;
import com.atmire.utils.helper.MetadataFieldString;
import com.atmire.utils.subclasses.MetadatumExtended;
import org.apache.commons.collections.CollectionUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Metadatum;

import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 07 Jun 2018
 */
public abstract class AbstractFillValue implements EditMetadata {

    private String field;
    private boolean overwrite = false;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    protected List<Metadatum> convert(List<String> values) {
        LinkedList<Metadatum> metadata = new LinkedList<>();
        for (String value : values) {
            metadata.add(new MetadatumExtended(getField(), value));
        }
        return metadata;
    }

    protected List<Metadatum> convertField(List<Metadatum> values) {
        LinkedList<Metadatum> metadata = new LinkedList<>();
        for (Metadatum value : values) {
            MetadatumExtended metadatum = MetadataFieldString.encapsulate(getField())
                    .filledWith(value);
            metadata.add(metadatum);
        }
        return metadata;
    }

    public abstract List<Metadatum> getValues(EditParameters parameters);

    @Override
    public void run(FillMetadataRunner parameters) {
        DSpaceObject dso = parameters.getObject();
        List<Metadatum> values = getValues(parameters);
        if (isNotEmpty(values)) {
            if (isOverwrite()) {
                MetadataUtils.clearMetadata(dso, getField());
                MetadataUtils.addAllMetadata(dso, values);
            } else if (isEmpty(MetadataUtils.getMetadata(dso, getField()))) {
                MetadataUtils.addAllMetadata(dso, values);
            }
        }
    }

}
