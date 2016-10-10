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
    private static final String QUERY_OR_INTERJECTION = " OR ";

    @Autowired
    private SearchService searchService;

    @Override
    public Map<String, Collection> retrieveRelatedItems(Item item,Context context) throws SearchServiceException {
        Map<String, Collection> matchingItems = new HashMap<>();

        Set<ItemMetadataRelation> searchableRelations = retrieveItemRelations(true, false);

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
        ItemMetadataRelation inverseMetadataRelation = metadataRelation.createInverseMetadataRelation();

        inverseMetadataRelation.setSourceMetadataField(metadataRelation.getDestinationMetadataField());
        addMatchingItems(item, context, matchingItems, inverseMetadataRelation);

        return matchingItems;
    }

    private void addMatchingItems(Item item, Context context, Map<String, Collection> matchingItems, ItemMetadataRelation metadataRelation) throws SearchServiceException {
        DiscoverQuery query = new DiscoverQuery();

        String queryString = createQueryStringFromRelation(item, metadataRelation,false);
        String inverseQueryString = createQueryStringFromRelation(item, metadataRelation, true);

        List<DSpaceObject> relatedItems = retrieveRelatedItems(context, item, query, queryString);
        List<DSpaceObject> inverseRelatedItems = retrieveRelatedItems(context, item, query, inverseQueryString);

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
        List<DSpaceObject> relatedItems=null;
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

    private String createQueryStringFromRelation(Item item, ItemMetadataRelation metadatum, boolean useSourceFilterFacet) {
        DiscoverySearchFilterFacet destinationFilterFacet =metadatum.getDestinationFilterFacet();
        if(useSourceFilterFacet){
            destinationFilterFacet= metadatum.getSourceFilterFacet();
        }
        String destinationMetadataField = destinationFilterFacet.getIndexFieldName()+ QUERY_KEYWORD;
        String sourceMetadataField = metadatum.getSourceMetadataField();

        return generateQueryString(item, sourceMetadataField, destinationMetadataField);
    }

    private String createQueryStringFromRelation(Metadatum[] metadataFromItem, ItemMetadataRelation metadatum) {
        String destinationMetadataField = metadatum.getSourceFilterFacet().getIndexFieldName()+ QUERY_KEYWORD;
        return generateQueryString(metadataFromItem, destinationMetadataField);
    }


    public Map<String, Collection<Metadatum>> retrieveInverseRelationMetadata(Context context, Metadatum[] metadatum) throws SearchServiceException {

        Set<ItemMetadataRelation> searchableRelations = retrieveItemRelations(false,false);

        Map<String, Collection<Metadatum>> matchingItems= new HashMap<>();

        for (ItemMetadataRelation itemRelation : searchableRelations) {
            DiscoverQuery query = new DiscoverQuery();

            String queryString = createQueryStringFromRelation(metadatum, itemRelation);

            List<DSpaceObject> relatedItems = retrieveRelatedItems(context, null, query, queryString);

            if (CollectionUtils.isNotEmpty(relatedItems)) {
                ArrayList<Metadatum> tempList = new ArrayList<>();
                for(DSpaceObject dspaceObject : relatedItems){
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
    private String generateQueryString(Metadatum[] metadataFromItem, String destinationMetadatafield) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int  i = 0 ; i < metadataFromItem.length;i++){
            stringBuffer.append(destinationMetadatafield+":\""+metadataFromItem[i].value+"\"");
            if(i+1 < metadataFromItem.length){
                stringBuffer.append(QUERY_OR_INTERJECTION);
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