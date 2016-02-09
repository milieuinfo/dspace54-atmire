package com.atmire.lne.content;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.*;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Implementation of {@link ItemService}
 */
public class ItemServiceBean implements ItemService {

    private static final String HANDLE_SEARCH_FIELD = "handle";
    public static final String EXTERNAL_HANDLE_DISCOVERY_FIELD = "externalHandle";

    private SearchService searchService = null;

    public List<Item> findItemsByExternalHandle(final Context context, final String externalHandle) throws SearchServiceException {

        DiscoverResult result = getSearchService().search(context, buildDiscoveryQuery(EXTERNAL_HANDLE_DISCOVERY_FIELD,
                externalHandle));

        return FluentIterable.from(result.getDspaceObjects())
                .transform(new Function<DSpaceObject, Item>() {
                    @Nullable
                    public Item apply(@Nullable final DSpaceObject input) {
                        if (input instanceof Item) {
                            return (Item) input;
                        } else {
                            return null;
                        }
                    }
                }).filter(new Predicate<Item>() {
                    public boolean apply(@Nullable final Item input) {
                        return input != null;
                    }
                }).toList();
    }

    private DiscoverQuery buildDiscoveryQuery(final String metadataField, final String metadataValue) {
        DiscoverQuery query = new DiscoverQuery();
        query.setDSpaceObjectFilter(Constants.ITEM);
        query.setQuery(metadataField + "_keyword : " + metadataValue);
        query.addSearchField(HANDLE_SEARCH_FIELD);

        return query;
    }

    private SearchService getSearchService() {
        if (searchService == null) {
            searchService = SearchUtils.getSearchService();
        }
        return searchService;
    }
}
