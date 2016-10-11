package com.atmire.discovery;

import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by jonas - jonas@atmire.com on 13/05/16.
 */
public interface DiscoveryRelatedItemsService {

    Map<String, Collection> retrieveRelatedItems(Item item,Context context) throws SearchServiceException;

    Map<String, Collection> retrieveRelatedItems(Item item,Context context, String relationsName) throws SearchServiceException;

    Map<String, Collection<Metadatum>> retrieveInverseRelationMetadata(Context context, Item item) throws SearchServiceException;

    Set<ItemMetadataRelation> retrieveItemRelations();

}