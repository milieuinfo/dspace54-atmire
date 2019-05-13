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
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Created by jonas - jonas@atmire.com on 13/05/16.
 */
public class DiscoveryRelatedItemsServiceBean extends AbstractDiscoveryRelatedItemsService implements DiscoveryRelatedItemsService {

    private static final String QUERY_TO_INTERJECTION = "-TO-";
    private static final String QUERY_KEYWORD = "_keyword";
    private static final String QUERY_EXACT = "_exact";
    private static final String QUERY_OR_INTERJECTION = " OR ";

    @Autowired
    private SearchService searchService;

    @Override
    public Map<String, Collection> retrieveRelatedItems(Item item,Context context) throws SearchServiceException {
        Map<String, Collection> matchingItems = new HashMap<>();

        Set<ItemMetadataRelation> searchableRelations = retrieveItemRelations();

        for (ItemMetadataRelation metadatum : searchableRelations) {
            addMatchingItems(item, context, matchingItems, metadatum);
        }
        return matchingItems;
    }

    @Override
    public Map<String, Collection> retrieveRelatedItems(Item item,Context context, String relationsName) throws SearchServiceException {
        Map<String, Collection> matchingItems = new HashMap<>();

        ItemMetadataRelation metadataRelation = new DSpace().getServiceManager().getServiceByName(relationsName, ItemMetadataRelation.class);
        addMatchingItems(item, context, matchingItems, metadataRelation);

        return matchingItems;
    }

    private void addMatchingItems(Item item, Context context, Map<String, Collection> matchingItems, ItemMetadataRelation metadataRelation) throws SearchServiceException {
        DiscoverQuery query = new DiscoverQuery();

        //First check the hard set child to parent relation
        String queryString = createQueryStringFromRelation(item, metadataRelation, false);
        List<DSpaceObject> relatedItems = retrieveRelatedItems(context, item, query, queryString);

        //Then calculate "virtual" parent to child relations
        String inverseQueryString = createQueryStringFromRelation(item, metadataRelation, true);
        List<DSpaceObject> inverseRelatedItems = retrieveRelatedItems(context, item, query, inverseQueryString);

        //Then check if any parent to child relations are hard set in the database, and also add those
        if(metadataRelation.hasInverseRelationField()) {
            ItemMetadataRelation inverseFieldRelation = metadataRelation.createInverseFieldRelation();
            String inverseFieldQueryString = createQueryStringFromRelation(item, inverseFieldRelation, false);
            List<DSpaceObject> itemsByInverseField = retrieveRelatedItems(context, item, query, inverseFieldQueryString);
            inverseRelatedItems.addAll(itemsByInverseField);
        }

        String key = metadataRelation.getSourceMetadataField() + QUERY_TO_INTERJECTION + metadataRelation.getDestinationMetadataField();
        addRetrievedRelatedItems(matchingItems, relatedItems, key);

        key = metadataRelation.getDestinationMetadataField() + QUERY_TO_INTERJECTION + metadataRelation.getSourceMetadataField();
        addRetrievedRelatedItems(matchingItems, inverseRelatedItems, key);
    }

    private void addRetrievedRelatedItems(Map<String, Collection> matchingItems, List<DSpaceObject> relatedItems, String key) {
        if (CollectionUtils.isNotEmpty(relatedItems)) {
            if(matchingItems.containsKey(key)){
                relatedItems.addAll(matchingItems.get(key));
            }
            matchingItems.put(key, relatedItems);
        }
    }

    private List<DSpaceObject> retrieveRelatedItems(Context context, Item item, DiscoverQuery query, String queryString) throws SearchServiceException {
        List<DSpaceObject> relatedItems = Collections.emptyList();
        if(StringUtils.isNotBlank(queryString)) {
            query.setQuery(queryString);
            if(item!=null){
                query.addFilterQueries("-search.resourceid:" + item.getID());
            }
            query.addFilterQueries("search.resourcetype:2");
            query.setMaxResults(9999);
            DiscoverResult result = searchService.search(context, query);

            relatedItems = result.getDspaceObjects();
        }
        return relatedItems;
    }

    private String createQueryStringFromRelation(Item item, ItemMetadataRelation metadatum, boolean inverse) {
        String sourceMetadataField = null;
        String destinationMetadataField = null;

        if(inverse){
            DiscoverySearchFilterFacet filterFacet = metadatum.getSourceFilterFacet();
            if(filterFacet == null) {
                destinationMetadataField = metadatum.getSourceMetadataField() + QUERY_EXACT;
            } else {
                destinationMetadataField = filterFacet.getIndexFieldName() + QUERY_KEYWORD;
            }

            sourceMetadataField = metadatum.getDestinationMetadataField();
        } else {
            DiscoverySearchFilterFacet filterFacet = metadatum.getDestinationFilterFacet();
            if(filterFacet == null) {
                destinationMetadataField = metadatum.getDestinationMetadataField() + QUERY_EXACT;
            } else {
                destinationMetadataField = filterFacet.getIndexFieldName() + QUERY_KEYWORD;
            }

            sourceMetadataField = metadatum.getSourceMetadataField();
        }

        return generateQueryString(item, sourceMetadataField, destinationMetadataField);
    }

    public Map<String, Collection<Metadatum>> retrieveInverseRelationMetadata(Context context, Item item) throws SearchServiceException {

        Set<ItemMetadataRelation> searchableRelations = retrieveItemRelations();

        Map<String, Collection<Metadatum>> matchingItems = new HashMap<>();

        for (ItemMetadataRelation itemRelation : searchableRelations) {
            DiscoverQuery query = new DiscoverQuery();

            String inverseQueryString = createQueryStringFromRelation(item, itemRelation, true);
            List<DSpaceObject> inverseRelatedItems = retrieveRelatedItems(context, item, query, inverseQueryString);

            if (CollectionUtils.isNotEmpty(inverseRelatedItems)) {
                HashSet<Metadatum> tempList = new HashSet<>();

                for(DSpaceObject dspaceObject : inverseRelatedItems){
                    tempList.addAll(Arrays.asList(dspaceObject.getMetadataByMetadataString(itemRelation.getDestinationMetadataField())));
                }

                matchingItems.put(itemRelation.getInverseRelationField(), tempList);
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
                stringBuffer.append(QUERY_OR_INTERJECTION);
            }
        }
        return stringBuffer.toString();
    }

}