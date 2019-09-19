package com.atmire.metadata;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 07 Jun 2018
 */
public class AutoFillMetadataConsumer implements Consumer {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(AutoFillMetadataConsumer.class);

    private Set<Integer> itemIDs = new HashSet<>();

    private Set<String> enabledCollections = new HashSet<>();

    public void initialize() throws Exception {
        String[] collectionHandles = StringUtils.split(ConfigurationManager.getProperty("autofill", "autofill.metadata.consumer.collections"), ",");
        if (collectionHandles != null && collectionHandles.length > 0) {
            for (String collectionHandle : collectionHandles) {
                enabledCollections.add(StringUtils.trim(collectionHandle));
            }
        }
    }

    /**
     * Gather the DspaceObject IDs here.
     * DO NOT COMMIT THE CONTEXT
     */
    public void consume(Context context, Event event) throws Exception {

        int subjectType = event.getSubjectType();
        int eventType = event.getEventType();
        int subjectID = event.getSubjectID();
        int objectID = event.getObjectID();

        DSpaceObject dso = event.getSubject(context);

        switch (subjectType) {
            case Constants.ITEM:
                if (eventType == Event.INSTALL || eventType == Event.MODIFY || eventType == Event.MODIFY_METADATA) {
                    itemIDs.add(subjectID);
                }
                break;
            default:
                log.warn("consume() got unrecognized event: " + event.toString());
        }

    }

    /**
     * Find the objects based on the IDS.
     * Process them here.
     * commit and clear the IDs
     */
    public void end(Context context) throws Exception {
        try {
            if (CollectionUtils.isNotEmpty(itemIDs)) {
                for (Integer itemID : itemIDs) {
                    try {
                        Item item = Item.find(context, itemID);
                        Collection owningCollection = getItemCollection(context, item);
                        if (item != null && shouldAutoFillInCollection(owningCollection)) {
                            //noinspection unchecked
                            List<EditMetadata> config = new DSpace().getServiceManager()
                                    .getServiceByName("autoFillMetadata", List.class);
                            new FillMetadataRunner(config, item).run();
                            item.update();
                        }
                    } catch (Exception e) {
                        log.error("", e);
                    }
                }
                context.getDBConnection().commit();
            }
        } finally {
            itemIDs.clear();
        }
    }

    private Collection getItemCollection(Context context, Item item) throws SQLException {
        Collection owningCollection = null;

        if (item != null) {
            owningCollection = item.getOwningCollection();
            if (owningCollection == null) {
                WorkspaceItem workspaceItem = WorkspaceItem.findByItem(context, item);
                if (workspaceItem != null) {
                    owningCollection = workspaceItem.getCollection();
                }
            }

            if (owningCollection == null) {
                WorkflowItem workflowItem = WorkflowItem.findByItem(context, item);
                if (workflowItem != null) {
                    owningCollection = workflowItem.getCollection();
                }
            }

            if (owningCollection == null) {
                XmlWorkflowItem workflowItem = XmlWorkflowItem.findByItem(context, item);
                if (workflowItem != null) {
                    owningCollection = workflowItem.getCollection();
                }
            }
        }

        return owningCollection;
    }

    public void finish(Context ctx) throws Exception {

    }

    private boolean shouldAutoFillInCollection(final Collection owningCollection) {
        if (CollectionUtils.isEmpty(enabledCollections) || (owningCollection != null && enabledCollections.contains(owningCollection.getHandle()))) {
            return true;
        } else {
            return false;
        }
    }
}
