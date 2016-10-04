package com.atmire.ctask.general;

import com.atmire.discovery.DiscoveryRelatedItemsService;
import com.atmire.discovery.ItemMetadataRelation;
import java.sql.*;
import java.util.Collection;
import org.apache.commons.collections.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.*;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

import java.io.IOException;
import java.util.*;

/**
 * @author philip at atmire.com
 */
public class RelatedItemsCurationTask extends AbstractCurationTask {

    private static final Logger log =  Logger.getLogger(RelatedItemsCurationTask.class);

    private DiscoveryRelatedItemsService relatedItemsService = new DSpace().getServiceManager().getServiceByName("DiscoveryRelatedItemsService", DiscoveryRelatedItemsService.class);

    private Set<ItemMetadataRelation> configuredRelations = new DSpace().getServiceManager().getServiceByName("item-relations", Set.class);

    private int status;

    private Map<String, Set<String>> identifierWithoutItemMap;
    private Map<String, Set<String>> identifierWithMultipleItemMap;

    @Override
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);
        status = Curator.CURATE_SUCCESS;
        identifierWithoutItemMap = new HashMap<>();
        identifierWithMultipleItemMap = new HashMap<>();
    }

    @Override
    public int perform(DSpaceObject dso) throws IOException {
        if (dso.getType() == Constants.ITEM)
        {
            Item item = (Item) dso;

            Context context = null;
            try {
                context = new Context();
                context.turnOffAuthorisationSystem();

                checkRelatedItemsExist(context, item);
                checkIdentifierIsUnique(context, item);
                
            } catch (Exception e) {
                log.error(e.getMessage(),e);
                return Curator.CURATE_ERROR;
            } finally {
                if(context!=null && context.isValid()){
                    context.abort();
                }
            }
        }

        processResults();
        return status;
    }

    private void checkIdentifierIsUnique(Context context, Item item) throws SQLException, IOException, AuthorizeException {
        for (ItemMetadataRelation configuredRelation : configuredRelations) {
            String identifier = item.getMetadata(configuredRelation.getDestinationMetadataField());

            if (StringUtils.isNotBlank(identifier) && !identifierWithMultipleItemMap.containsKey(identifier)) {
                String[] split = StringUtils.split(configuredRelation.getDestinationMetadataField(),".");

                String schema = split[0];
                String element = split[1];
                String qualifier = null;

                if(split.length==3){
                    qualifier = split[2];
                }

                ItemIterator itemIterator = Item.findByAuthorityValue(context, schema, element, qualifier, identifier);

                HashSet<String> itemHandles = new HashSet<>();

                while (itemIterator.hasNext()){
                    Item next = itemIterator.next();
                    itemHandles.add(next.getHandle());
                }

                if(itemHandles.size() > 1) {
                    identifierWithMultipleItemMap.put(identifier, itemHandles);
                    status = Curator.CURATE_FAIL;
                }
            }
        }
    }

    private void checkRelatedItemsExist(Context context, Item item) throws SearchServiceException {
        List<Item> relatedItems = getRelatedItems(context, item);

        for (ItemMetadataRelation configuredRelation : configuredRelations) {
            String identifier = item.getMetadata(configuredRelation.getSourceMetadataField());

            if (StringUtils.isNotBlank(identifier)) {
                List<String> matchingItemHandles = new ArrayList<>();

                for (Item relatedItem : relatedItems) {
                    String metadata = relatedItem.getMetadata(configuredRelation.getDestinationMetadataField());

                    if (identifier.equals(metadata)) {

                        matchingItemHandles.add(relatedItem.getHandle());
                    }
                }

                if (matchingItemHandles.size() == 0) {
                    addHandleToMap(identifierWithoutItemMap, identifier, item.getHandle());
                    status = Curator.CURATE_FAIL;
                }
            }
        }
    }

    private List<Item> getRelatedItems(Context context, Item item) throws SearchServiceException {
        java.util.List<Item> relatedItems = new ArrayList<>();

        Map<String, Collection> relatedMetadata = relatedItemsService.retrieveRelatedItems(item, context);


        Iterator<Map.Entry<String,Collection>> it = relatedMetadata.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String,Collection> pair = it.next();

            Collection collection = pair.getValue();

            relatedItems.addAll(collection);
        }

        return relatedItems;
    }

    private void processResults() throws IOException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Related Items Report: \n----------------\n");

        addMapToResults(identifierWithoutItemMap, sb, "No item found for identifier");
        addMapToResults(identifierWithMultipleItemMap, sb, "Multiple items found for identifier");

        setResult(sb.toString());
        report(sb.toString());
    }

    private void addHandleToMap(Map<String, Set<String>> map, String identifier, String handle){
        if(!map.containsKey(identifier)){
            map.put(identifier, new HashSet<String>());
        }

        map.get(identifier).add(handle);
    }

    private void addMapToResults(Map<String, Set<String>> map, StringBuilder sb, String message){
        for (String identifier : map.keySet()) {
            Set<String> itemHandles = map.get(identifier);

            if(CollectionUtils.isNotEmpty(itemHandles)) {
                sb.append(message + " \"" + identifier + "\". The impacted items are: ").append("\n");

                for (String handle : itemHandles) {
                    sb.append(" - ").append(handle).append("\n");
                }
                sb.append("\n");
            }
        }
    }
}
