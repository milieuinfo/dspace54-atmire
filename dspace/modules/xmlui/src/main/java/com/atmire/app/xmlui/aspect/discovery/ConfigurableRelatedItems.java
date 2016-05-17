package com.atmire.app.xmlui.aspect.discovery;

import com.atmire.discovery.DiscoveryRelatedItemsService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
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

    private static final Message T_head = message("xmlui.Discovery.RelatedItems.head");
    private static final Message T_related_help = message("xmlui.Discovery.ConfigurableRelatedItems.help");

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
        Map<Metadatum,Collection> relatedMetadata = null;
        try {
            relatedMetadata = relatedItemsService.retrieveRelatedItems(item, context);
        } catch (SearchServiceException e) {
            log.error("Error retrieving related items.",e);
        }


        if(MapUtils.isNotEmpty(relatedMetadata))
        {
            java.util.List<Item> relatedItems =new ArrayList<>();
            Iterator<Map.Entry<Metadatum,Collection>> it = relatedMetadata.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<Metadatum,Collection> pair = it.next();

                Collection collection = pair.getValue();
                relatedItems.addAll(collection);
            }

            if(CollectionUtils.isNotEmpty(relatedItems))
            {
                Division mltDiv = body.addDivision("item-related-container").addDivision("item-related", "secondary related");
                mltDiv.setHead(T_head);

                mltDiv.addPara(T_related_help);

                ReferenceSet set = mltDiv.addReferenceSet(
                        "item-related-items", ReferenceSet.TYPE_SUMMARY_LIST,
                        null, "related-items");

                for (DSpaceObject dso : relatedItems)
                {
                    set.addReference(dso);
                }
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
