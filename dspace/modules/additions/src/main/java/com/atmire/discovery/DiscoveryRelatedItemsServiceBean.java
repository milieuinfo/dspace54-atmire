package com.atmire.discovery;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Created by jonas - jonas@atmire.com on 13/05/16.
 */
public class DiscoveryRelatedItemsServiceBean implements DiscoveryRelatedItemsService {


    @Autowired
    private SearchService searchService;

    @Override
    public Map<Metadatum, Collection> retrieveRelatedItems(Item item, Context context) throws SearchServiceException {
        Map<Metadatum, Collection> matchingItems = new HashMap<>();

        Set<String> configuredMetadata = new DSpace().getServiceManager().getServiceByName("item-relations", Set.class);

        for (String metadatum : configuredMetadata) {
            DiscoverQuery query = new DiscoverQuery();
            Metadatum[] metadataFromItem = item.getMetadataByMetadataString(metadatum);
            String queryString = "";
            for (int  i = 0 ; i < metadataFromItem.length;i++){
                queryString+=metadatum+":\""+metadataFromItem[i].value+"\"";
                if(i+1 < metadataFromItem.length){
                    queryString+=" OR ";
                }
            }
            List<DSpaceObject> relatedItems=null;
            if(StringUtils.isNotBlank(queryString)){
                query.setQuery(queryString);
                query.addFilterQueries("-search.resourceid:" + item.getID(),"search.resourcetype:2");
                DiscoverResult result = searchService.search(context, query);

                relatedItems = result.getDspaceObjects();
            }

            if (CollectionUtils.isNotEmpty(relatedItems)) {

                Metadatum newMetadatum = createMetadatumFromString(metadatum);
                matchingItems.put(newMetadatum, relatedItems);
            }

        }
        return matchingItems;
    }

    private Metadatum createMetadatumFromString(String metadatum) {
        Metadatum newMetadatum = new Metadatum();
        String[] metadataSplit = metadatum.split("\\.");
        newMetadatum.schema = metadataSplit[0];
        newMetadatum.element = metadataSplit[1];
        if (metadataSplit.length == 3) {
            newMetadatum.qualifier = metadataSplit[2];
        }
        return newMetadatum;
    }
}