package com.atmire.app.xmlui.itemcloning;

import com.atmire.utils.MetadataUtils;
import com.atmire.utils.NullValidation;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.lang.UnhandledException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 19 Jun 2018
 */
public class CloneAction extends AbstractAction {

    private static final Logger log = LogManager.getLogger(CloneAction.class);

    public Map act(
            Redirector redirector,
            SourceResolver sourceResolver,
            Map objectModel,
            String s,
            Parameters parameters
    ) throws Exception {
        Context context = ContextUtil.obtainContext(objectModel);
        context.setDispatcher("noautofill");

        Item item = CloneItemUtils.getItem(context, objectModel);
        NullValidation<Collection> objectValidation = new NullValidation<>()
                .take(item, "The item can't be found")
                .thenTake(getOwningCollection(), "The item has no owning collection");
        if (objectValidation.isOK()) {
            Collection owningCollection = objectValidation.getObject();
            String parameter = parameters.getParameter("action");
            NullValidation<Action> actionValidation = new NullValidation<>()
                    .take(parameter, "The action parameter is blank")
                    .thenTake(getAction(), "The action parameter has no valid value");
            if (actionValidation.isOK()) {
                Action action = actionValidation.getObject();
                String workspaceID = createNewItem(context, owningCollection, item, action);
                return ImmutableMap.of(
                        "workspaceID", workspaceID,
                        "handle", owningCollection.getHandle()
                );
            } else {
                throw new IllegalArgumentException(actionValidation.getCharacters());
            }
        } else {
            throw new IllegalArgumentException(objectValidation.getCharacters());
        }
    }

    private Function<String, Action> getAction() {
        return new Function<String, Action>() {
            @Override
            public Action apply(String input) {
                try {
                    return Enum.valueOf(Action.class, input.toUpperCase());
                } catch (Exception e) {
                    log.error("", e);
                    return null;
                }
            }
        };
    }

    private Function<Item, Collection> getOwningCollection() {
        return new Function<Item, Collection>() {
            @Override
            public Collection apply(Item input) {
                try {
                    return input.getOwningCollection();
                } catch (SQLException e) {
                    throw new UnhandledException(e);
                }
            }
        };
    }

    private String createNewItem(
            Context context,
            Collection owningCollection,
            Item item,
            Action action
    ) {
        try {
            WorkspaceItem workspaceItem = WorkspaceItem.create(context, owningCollection, false);
            Item newItem = workspaceItem.getItem();
            for (Metadatum m : item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY)) {
                newItem.addMetadata(m.schema, m.element, m.qualifier, m.language, m.value);
            }
            if (action == Action.ONDERDEEL) {
                clearFields(newItem);
                MetadataUtils.addMetadata
                        (newItem, "dc.relation.ispartof", getIdentifier(item));
                inheritType(newItem, item);
            }
            if (action == Action.VERSION) {
                clearFields(newItem);
                MetadataUtils.addMetadata
                        (newItem, "dc.relation.replaces", getIdentifier(item));
            }
            newItem.update();
            return String.valueOf(workspaceItem.getID());
        } catch (AuthorizeException | SQLException | IOException e) {
            throw new UnhandledException(e);
        }
    }

    private void clearFields(Item newItem) {
        for (String field : getFieldsToWipe()) {
            MetadataUtils.clearMetadata(newItem, field);
        }
    }

    private Set<String> getFieldsToWipe() {
        HashSet<String> fields = new HashSet<>();
        Collections.addAll(fields, "dc.relation.ispartof", "dc.relation.replaces");
        Collections.addAll(fields, getWipeFields());
        return fields;
    }

    private String[] getWipeFields() {
        return getCommaSeparatedValues("clone.item.metadata.wipe");
    }

    private String[] getCommaSeparatedValues(String property) {
        String config = ConfigurationManager.getProperty(property);
        return (config == null ? "" : config).split(",");
    }

    private String getIdentifier(Item item) {
        return MetadataUtils.getMetadataFirstValue(item, "vlaanderen.identifier");
    }

    private void inheritType(Item newItem, Item originalItem) {
        String type = MetadataUtils.getMetadataFirstValue(originalItem, "dc.type");
        String[] config = getCommaSeparatedValues("clone.item.metadata.type.hierarchy");
        String newType = getTypeMap(config).get(toLowerCase(type));
        if (StringUtils.isNotBlank(newType)) {
            MetadataUtils.clearMetadata(newItem, "dc.type");
            MetadataUtils.addMetadata(newItem, "dc.type", newType);
        }
    }

    private Map<String, String> getTypeMap(String[] commaSeparatedValues) {
        Map<String, String> typeMap = new HashMap<>();
        String previousType = null;
        for (String type : commaSeparatedValues) {
            typeMap.put(toLowerCase(previousType), type);
            previousType = type;
        }
        return typeMap;
    }

    private String toLowerCase(String previousType) {
        return previousType != null ? previousType.toLowerCase() : null;
    }
}
