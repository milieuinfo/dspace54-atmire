package com.atmire.lne.content;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

import java.util.List;

/**
 * Service that can resolve items for LNE
 */
public interface ItemService {

    List<Item> findItemsByExternalHandle(final Context context, String externalHandle) throws SearchServiceException;

}
