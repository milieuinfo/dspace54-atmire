package com.atmire.content.authority;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
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
        System.out.println("consuming some events");

        DSpaceObject dso = event.getSubject(context);
        Item submittedItem = (Item) dso;

        submittedItem.getMetadata("vlaanderen.identifier");


    }


    /**
     * Commit here.
     * @param ctx
     * @throws Exception
     */
    public void end(Context ctx) throws Exception
    {

    }

    public void finish(Context ctx) throws Exception
    {

    }
}
