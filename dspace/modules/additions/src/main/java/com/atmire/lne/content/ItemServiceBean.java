package com.atmire.lne.content;

import com.atmire.lne.exception.MetaDataFieldNotSetException;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
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
    public static final String LNE_CONFIGURATION_MODULE = "lne";
    public static final String EXTERNAL_HANDLE_METADATA_FIELD = "external.handle.metadata.field";

    private SearchService searchService = null;

    public List<Item> findItemsByExternalHandle(final Context context, final String externalHandle) throws SearchServiceException, MetaDataFieldNotSetException {
        String metadataField = getMetadataField();

        DiscoverResult result = getSearchService().search(context, buildDiscoveryQuery(metadataField, externalHandle));

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

    protected String getMetadataField() throws MetaDataFieldNotSetException {
        String metaDataField = ConfigurationManager.getProperty(LNE_CONFIGURATION_MODULE, EXTERNAL_HANDLE_METADATA_FIELD);
        if (StringUtils.isBlank(metaDataField)) {
            throw new MetaDataFieldNotSetException("Configuration property " + EXTERNAL_HANDLE_METADATA_FIELD
                    + " does not contain a valid value.");
        }
        return metaDataField;
    }

    private DiscoverQuery buildDiscoveryQuery(final String metadataField, final String metadataValue) {
        DiscoverQuery query = new DiscoverQuery();
        query.setDSpaceObjectFilter(Constants.ITEM);
        query.setQuery(metadataField + " : " + metadataValue);
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
