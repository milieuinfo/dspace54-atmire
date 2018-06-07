package com.atmire.metadata;

import com.atmire.utils.MetadataUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Metadatum;

import java.util.List;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 07 Jun 2018
 */
public class FillMetadataRunner implements FillValueDependencies {

    private final List<FillValue> config;
    private final DSpaceObject dso;

    public FillMetadataRunner(List<FillValue> config, DSpaceObject dso) {
        this.config = config;
        this.dso = dso;
    }

    public void run() {
        if (dso != null) {
            for (FillValue fillValue : config) {
                String field = fillValue.getField();
                List<Metadatum> metadata = MetadataUtils.getMetadata(dso, field);
                if (metadata.isEmpty()) {
                    String value = fillValue.getValue(this);
                    MetadataUtils.addMetadata(dso, field, value);
                }
            }
        }
    }

    @Override
    public DSpaceObject getObject() {
        return dso;
    }
}
