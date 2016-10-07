package com.atmire.app.xmlui.aspect.discovery;

import com.atmire.discovery.DiscoveryRelatedItemsService;
import com.atmire.discovery.ItemMetadataRelation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;


/**
 * Created by jonas - jonas@atmire.com on 13/05/16.
 */
public class ConfigurableRelatedItems extends AbstractDSpaceTransformer{

    /* Log4j logger*/
    private static final Logger log =  Logger.getLogger(ConfigurableRelatedItems.class);

    private static final String METADATAFIELD_HEADER_BASE = "xmlui.Discovery.ConfigurableRelatedItems.head_";
    private static final String METADATAFIELD_MESSAGE_BASE = "xmlui.Discovery.ConfigurableRelatedItems.help_";

    /**
     * Display items related to the given item
     */
    public void addBody(Body body) throws SAXException, WingException,
            SQLException, IOException, AuthorizeException
    {

        DSpaceObject dspaceObject = HandleUtil.obtainHandle(objectModel);
        if (!(dspaceObject instanceof Item))
        {
            return;
        }
        Item item = (Item) dspaceObject;

        DiscoveryRelatedItemsService relatedItemsService = new DSpace().getServiceManager().getServiceByName("DiscoveryRelatedItemsService", DiscoveryRelatedItemsService.class);
        Set<ItemMetadataRelation> configuredRelations = new DSpace().getServiceManager().getServiceByName("item-relations", Set.class);

        for(ItemMetadataRelation metadataRelation : configuredRelations){
            String key = metadataRelation.getSourceMetadataField() + "-TO-" + metadataRelation.getDestinationMetadataField();
            try {
                Map<String,Collection> relatedMetadata = relatedItemsService.retrieveRelatedItems(item,context, key);
                if(MapUtils.isNotEmpty(relatedMetadata))
                {
                    java.util.List<Item> relatedItems =new ArrayList<>();
                    if(relatedMetadata.isEmpty()) {
                        return;
                    }
                    Division partsDiv = body.addDivision(key, "secondary related");
                    partsDiv.setHead(message(METADATAFIELD_HEADER_BASE+ key));
                    Iterator<Map.Entry<String,Collection>> it = relatedMetadata.entrySet().iterator();
                    while(it.hasNext()){
                        Map.Entry<String,Collection> pair = it.next();

                        Collection collection = pair.getValue();
                        relatedItems.clear();
                        relatedItems.addAll(collection);
                        addRelatedItemsDiv(partsDiv , relatedItems, pair.getKey());
                    }
                }
            } catch (Exception e) {
              log.error("Error retrieving related items for: "+key, e);
            }
        }
    }


    private void addRelatedItemsDiv(Division division, List<Item> relatedItems, String metadataField) throws WingException {
        if(CollectionUtils.isNotEmpty(relatedItems))
        {

            division.addPara(null,"relation-headers").addContent(message(METADATAFIELD_MESSAGE_BASE +metadataField));

            ReferenceSet set = division.addReferenceSet(
                    "item-related-items", ReferenceSet.TYPE_SUMMARY_LIST,
                    null, "related-items");

            for (DSpaceObject dso : relatedItems)
            {
                set.addReference(dso);
            }
        }
    }

    /**
     * Recycle
     */
    public void recycle() {
        super.recycle();
    }
}
