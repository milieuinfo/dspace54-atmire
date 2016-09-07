package com.atmire.ctask.general;

import com.atmire.discovery.*;
import java.io.*;
import java.util.*;
import java.util.Collection;
import org.apache.commons.lang.*;
import org.apache.log4j.*;
import org.dspace.content.*;
import org.dspace.core.*;
import org.dspace.curate.*;
import org.dspace.discovery.*;
import org.dspace.utils.*;

/**
 * @author philip at atmire.com
 */
public class RelatedItemsCurationTask extends AbstractCurationTask {

    private static final Logger log =  Logger.getLogger(RelatedItemsCurationTask.class);

    private DiscoveryRelatedItemsService relatedItemsService = new DSpace().getServiceManager().getServiceByName("DiscoveryRelatedItemsService", DiscoveryRelatedItemsService.class);

    private Set<ItemMetadataRelation> configuredRelations = new DSpace().getServiceManager().getServiceByName("item-relations", Set.class);

    private int status;

    private Map<String, List<String>> identifierWithoutItemMap;
    private Map<String, List<String>> identifierWithMultipleItemMap;

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
            Context context = null;
            try {
                context = new Context();
                context.turnOffAuthorisationSystem();

                Item item = (Item) dso;
                List<Item> relatedItems = getRelatedItems(context, item);

                StringBuilder sb = new StringBuilder();

                for (ItemMetadataRelation configuredRelation : configuredRelations) {
                    String identifier = item.getMetadata(configuredRelation.getSourceMetadataField());

                    if(StringUtils.isNotBlank(identifier)) {
                        int matches = 0;

                        for (Item relatedItem : relatedItems) {
                            String metadata = relatedItem.getMetadata(configuredRelation.getDestinationMetadataField());

                            if(identifier.equals(metadata)){
                                matches+=1;
                            }
                        }

                        if(matches == 0) {
                            addHandleToMap(identifierWithoutItemMap, identifier, item.getHandle());
                            status = Curator.CURATE_FAIL;
                        }
                        if(matches > 1) {
                            addHandleToMap(identifierWithMultipleItemMap, identifier, item.getHandle());
                            status = Curator.CURATE_FAIL;
                        }
                    }
                }
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

    private List<Item> getRelatedItems(Context context, Item item) throws SearchServiceException {
        java.util.List<Item> relatedItems =new ArrayList<>();

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

    private void addHandleToMap(Map<String, List<String>> map, String identifier, String handle){
        if(!map.containsKey(identifier)){
            map.put(identifier, new ArrayList<String>());
        }

        map.get(identifier).add(handle);
    }

    private void addMapToResults(Map<String, List<String>> map, StringBuilder sb, String message){
        for (String identifier : map.keySet()) {
            sb.append(message + " \'" + identifier + "\" on these items: ").append("\n");

            for (String handle : map.get(identifier)) {
                sb.append(handle).append("\n");
            }
        }
    }
}
