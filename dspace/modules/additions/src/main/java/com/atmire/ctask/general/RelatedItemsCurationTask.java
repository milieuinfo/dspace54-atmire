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

    private List<String> results = new ArrayList<String>();

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
                            results.add("No item found for identifier " + identifier + " on item " + item.getHandle());
                        }
                        if(matches > 1) {
                            results.add("Multiple items found for identifier " + identifier + " on item " + item.getHandle());
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
        return Curator.CURATE_SUCCESS;
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
        for(String result : results)
        {
            sb.append(result).append("\n");
        }
        setResult(sb.toString());
        report(sb.toString());
    }
}
