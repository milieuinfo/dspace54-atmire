package com.atmire.discovery;

import org.dspace.utils.DSpace;

import java.util.Set;

/**
 * Created by jonas - jonas@atmire.com on 05/10/16.
 */
public abstract class AbstractDiscoveryRelatedItemsService implements DiscoveryRelatedItemsService {

    public Set<ItemMetadataRelation> retrieveItemRelations() {
        return new DSpace().getServiceManager().getServiceByName("item-relations", Set.class);
    }

}
