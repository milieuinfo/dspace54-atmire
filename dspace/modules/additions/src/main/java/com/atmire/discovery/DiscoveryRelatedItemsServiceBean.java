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
    public Map<String, Collection> retrieveRelatedItems(Item item, Context context) throws SearchServiceException {
        Map<String, Collection> matchingItems = new HashMap<>();

        Set<ItemMetadataRelation> configuredRelations = new DSpace().getServiceManager().getServiceByName("item-relations", Set.class);

        Set<ItemMetadataRelation> searchableRelations= new LinkedHashSet<>();
        for (ItemMetadataRelation metadatum : configuredRelations) {
            searchableRelations.add(metadatum);
            if(metadatum.isInverseRelationSearchEnabled()){
                searchableRelations.add(metadatum.createInverseMetadataRelation());
            }
        }

        for (ItemMetadataRelation metadatum : searchableRelations) {
            DiscoverQuery query = new DiscoverQuery();

            String destinationMetadataField = metadatum.getDestinationMetadataField();
            String sourceMetadataField = metadatum.getSourceMetadataField();

            String queryString = generateQueryString(item, sourceMetadataField, destinationMetadataField);

            List<DSpaceObject> relatedItems=null;
            if(StringUtils.isNotBlank(queryString)){
                query.setQuery(queryString);
                query.addFilterQueries("-search.resourceid:" + item.getID(),"search.resourcetype:2");
                DiscoverResult result = searchService.search(context, query);

                relatedItems = result.getDspaceObjects();
            }

            if (CollectionUtils.isNotEmpty(relatedItems)) {

                Metadatum newMetadatum = createMetadatumFromString(sourceMetadataField);
                matchingItems.put(newMetadatum.getField(), relatedItems);
            }

        }
        return matchingItems;
    }

    private String generateQueryString(Item item, String sourceMetadatafield, String destinationMetadatafield) {
        Metadatum[] metadataFromItem = item.getMetadataByMetadataString(sourceMetadatafield);
        StringBuffer stringBuffer = new StringBuffer();
        for (int  i = 0 ; i < metadataFromItem.length;i++){
            stringBuffer.append(destinationMetadatafield+":\""+metadataFromItem[i].value+"\"");
            if(i+1 < metadataFromItem.length){
                stringBuffer.append(" OR ");
            }
        }
        return stringBuffer.toString();
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