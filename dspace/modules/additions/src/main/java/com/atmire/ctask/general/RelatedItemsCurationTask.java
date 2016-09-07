package com.atmire.ctask.general;

import com.atmire.discovery.DiscoveryRelatedItemsService;
import com.atmire.discovery.ItemMetadataRelation;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
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

                for (ItemMetadataRelation configuredRelation : configuredRelations) {
                    String identifier = item.getMetadata(configuredRelation.getSourceMetadataField());

                    if(StringUtils.isNotBlank(identifier)) {
                        List<String> matchingItemHandles = new ArrayList<>();

                        for (Item relatedItem : relatedItems) {
                            String metadata = relatedItem.getMetadata(configuredRelation.getDestinationMetadataField());

                            if(identifier.equals(metadata)){

                                matchingItemHandles.add(relatedItem.getHandle());
                            }
                        }

                        if(matchingItemHandles.size() == 0) {
                            addHandleToMap(identifierWithoutItemMap, identifier, item.getHandle());
                            status = Curator.CURATE_FAIL;
                        }
                        if(matchingItemHandles.size() > 1) {
                            for (String matchingItemHandle : matchingItemHandles) {
                                addHandleToMap(identifierWithMultipleItemMap, identifier, matchingItemHandle);
                            }

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
            sb.append(message + " \"" + identifier + "\". The impacted items are: ").append("\n");

            for (String handle : map.get(identifier)) {
                sb.append(" - ").append(handle).append("\n");
            }
            sb.append("\n");
        }
    }
}
