package com.atmire.discovery;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.SolrServiceIndexPlugin;

import java.util.List;

/**
 * Indexer plugin that will index all metadata fields with the _exact suffix.
 */
public class ExactMatchIndexPlugin implements SolrServiceIndexPlugin {

    private static final String EXACT_MATCH_SUFFIX = "_exact";

    public void additionalIndex(final Context context, final DSpaceObject dso, final SolrInputDocument document) {

        Metadatum[] metadata = dso.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

        if(ArrayUtils.isNotEmpty(metadata)) {
            List<String> toIgnoreMetadataFields = SearchUtils.getIgnoredMetadataFields(dso.getType());

            for (Metadatum data : metadata) {
                String value = data.value;
                String field = buildField(data, toIgnoreMetadataFields);

                if (StringUtils.isBlank(value) || StringUtils.isBlank(field))
                {
                    continue;
                }

                document.addField(field + EXACT_MATCH_SUFFIX, value);
            }
        }

    }

    private String buildField(Metadatum meta, List<String> toIgnoreMetadataFields) {
        String field = meta.schema + "." + meta.element;
        String unqualifiedField = field;

        if (meta.qualifier != null && !meta.qualifier.trim().equals(""))
        {
            field += "." + meta.qualifier;
        }

        if (toIgnoreMetadataFields != null && (toIgnoreMetadataFields.contains(field) || toIgnoreMetadataFields.contains(unqualifiedField + "." + Item.ANY))) {
            return null;
        } else {
            return field;
        }
    }

}
