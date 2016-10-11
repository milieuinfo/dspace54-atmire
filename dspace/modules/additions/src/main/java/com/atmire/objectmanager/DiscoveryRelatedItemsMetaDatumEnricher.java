package com.atmire.objectmanager;

import com.atmire.discovery.DiscoveryRelatedItemsService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by jonas - jonas@atmire.com on 05/10/16.
 */
public class DiscoveryRelatedItemsMetaDatumEnricher implements MetaDatumEnricher {

    /* Log4j logger*/
    private static final Logger log = Logger.getLogger(DiscoveryRelatedItemsMetaDatumEnricher.class);

    @Autowired
    private DiscoveryRelatedItemsService discoveryRelatedItemsService;

    private String identifierField;

    @Override
    public void enrichMetadata(final Context context, final Item item, final List<Metadatum> metadataList) {
        if (CollectionUtils.isNotEmpty(metadataList)) {

            String thisItemsIdentifier = null;
            for (Metadatum metadatum : metadataList) {
                if (StringUtils.equals(identifierField, metadatum.getField())) {
                    thisItemsIdentifier = metadatum.value;
                    break;
                }
            }

            try {
                List<Metadatum> newMetaData = new LinkedList<>();
                //Get the list of "virtual" metadata fields for this item
                Map<String, Collection<Metadatum>> inverseMappingWithMetadata = discoveryRelatedItemsService.retrieveInverseRelationMetadata(context, item);

                //Add them to the metadata list
                for (String inverseMetadataField : inverseMappingWithMetadata.keySet()) {

                    Collection<Metadatum> inverseRelationMetdata = inverseMappingWithMetadata.get(inverseMetadataField);
                    for (Metadatum relationMetadatum : inverseRelationMetdata) {
                        if (StringUtils.equals(thisItemsIdentifier, relationMetadatum.value)) {
                            continue;
                        }
                        Metadatum m = new Metadatum();
                        String[] split = inverseMetadataField.split("\\.");
                        m.schema = split[0];
                        m.element = split[1];
                        m.qualifier = (split.length == 3) ? split[2] : null;
                        m.value = relationMetadatum.value;
                        newMetaData.add(m);
                    }
                }

                metadataList.addAll(newMetaData);

            } catch (SearchServiceException e) {
                log.error(e);
            }
        }

    }

    public String getIdentifierField() {
        return identifierField;
    }

    @Required
    public void setIdentifierField(String identifierField) {
        this.identifierField = identifierField;
    }
}
