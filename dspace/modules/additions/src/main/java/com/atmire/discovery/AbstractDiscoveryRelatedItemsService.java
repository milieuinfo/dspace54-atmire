package com.atmire.discovery;

import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by jonas - jonas@atmire.com on 05/10/16.
 */
public abstract class AbstractDiscoveryRelatedItemsService implements DiscoveryRelatedItemsService {


    @Override
    public abstract Map<String, Collection> retrieveRelatedItems(Item item, Context context) throws SearchServiceException;
    public abstract Map<String, Collection<Metadatum>> retrieveInverseRelationMetadata(Context context, Metadatum[] metadata) throws SearchServiceException;

    public Set<ItemMetadataRelation> retrieveItemRelations(boolean includeInvertedRelations, boolean defaultRelationReversal) {
        Set<ItemMetadataRelation> configuredRelations = new DSpace().getServiceManager().getServiceByName("item-relations", Set.class);

        Set<ItemMetadataRelation> searchableRelations = new LinkedHashSet<>();
        for (ItemMetadataRelation relation : configuredRelations) {
            searchableRelations.add(relation);
            if (relation.isInverseRelationSearchEnabled() && includeInvertedRelations) {
                ItemMetadataRelation inverseMetadataRelation = relation.createInverseMetadataRelation();
                if(defaultRelationReversal){
                    inverseMetadataRelation.setSourceMetadataField(relation.getDestinationMetadataField());
                }
                searchableRelations.add(inverseMetadataRelation);
            }
        }
        return searchableRelations;
    }
}
