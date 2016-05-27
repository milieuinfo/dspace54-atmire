package com.atmire.discovery;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

import java.util.Collection;
import java.util.Map;

/**
 * Created by jonas - jonas@atmire.com on 13/05/16.
 */
public interface DiscoveryRelatedItemsService {

    Map<String,Collection> retrieveRelatedItems(Item item, Context context) throws SearchServiceException;
}