package com.atmire.consumer;

import java.util.*;
import org.apache.commons.collections.*;
import org.dspace.content.*;
import org.dspace.core.*;
import org.dspace.event.*;
import org.dspace.utils.*;

/**
 * @author philip at atmire.com
 */
public class AsynchronousConsumersPool implements Consumer {

    HashSet<ConsumerDspaceObject> toUpdateDspaceObjects = new HashSet<>();

    private AsynchronousConsumerDispatcher asynchronousConsumerDispatcher =
            new DSpace().getServiceManager().getServicesByType(AsynchronousConsumerDispatcher.class).get(0);

    @Override
    public void initialize() throws Exception {
    }

    @Override
    public void consume(Context ctx, Event event) throws Exception {
        DSpaceObject dso = event.getSubject(ctx);

        if(dso!= null) {
            ConsumerDspaceObject consumerDspaceObject = new ConsumerDspaceObject(dso.getID(), dso.getType());

            toUpdateDspaceObjects.add(consumerDspaceObject);
        }


    }

    @Override
    public void end(Context ctx) throws Exception {
        if (CollectionUtils.isNotEmpty(toUpdateDspaceObjects)) {
            for (ConsumerDspaceObject toUpdateDspaceObject : toUpdateDspaceObjects) {
                asynchronousConsumerDispatcher.dispatch(toUpdateDspaceObject.getId(), toUpdateDspaceObject.getType());
            }

            toUpdateDspaceObjects.clear();
        }
    }

    @Override
    public void finish(Context ctx) throws Exception {

    }
}
