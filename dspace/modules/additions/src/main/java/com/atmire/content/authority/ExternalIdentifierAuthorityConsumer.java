package com.atmire.content.authority;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

/**
 * Created by dylan on 30/06/16.
 */
public class ExternalIdentifierAuthorityConsumer implements Consumer
{

    private static Logger log = Logger.getLogger(ExternalIdentifierAuthorityConsumer.class);
    public void initialize() throws Exception
    {

    }


    public void consume(Context context, Event event) throws Exception
    {

        // get the field
        String authorityField = ConfigurationManager.getProperty("authority.field");

        String[] splitField = authorityField.split("\\.");


        String dcSchema = splitField[0];
        String dcElement = splitField[1];
        String dcQualifier = null;
        if(splitField.length==3)
        {
            dcQualifier = splitField[2];
        }

        DSpaceObject dso = event.getSubject(context);
        Item submittedItem = (Item) dso;
        String identifierValue = submittedItem.getMetadata(authorityField);
        if(StringUtils.isNotEmpty(identifierValue))
        {
            if(StringUtils.isEmpty(submittedItem.getMetadataShallow(dcSchema,dcElement,dcQualifier,Item.ANY)[0].authority))
            {
                submittedItem.getMetadataShallow(dcSchema,dcElement, dcQualifier, Item.ANY)[0].authority = identifierValue;
                submittedItem.update();
            }
        }
    }


    /**
     * Commit here.
     * @param context
     * @throws Exception
     */
    public void end(Context context) throws Exception
    {
        context.commit();
    }

    public void finish(Context ctx) throws Exception
    {

    }
}
