package com.atmire.metadata;

import com.atmire.utils.MetadataUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Metadatum;

import java.util.List;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 07 Jun 2018
 */
public class FillMetadataRunner implements EditParameters {

    private final List<EditMetadata> config;
    private final DSpaceObject dso;

    public FillMetadataRunner(List<EditMetadata> config, DSpaceObject dso) {
        this.config = config;
        this.dso = dso;
    }

    public void run() {
        if (dso != null) {
            for (EditMetadata editMetadata : config) {
                editMetadata.run(this);
            }
        }
    }

    @Override
    public DSpaceObject getObject() {
        return dso;
    }
}
