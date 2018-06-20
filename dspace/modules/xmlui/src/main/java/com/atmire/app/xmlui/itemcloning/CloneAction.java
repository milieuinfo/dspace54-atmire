package com.atmire.app.xmlui.itemcloning;

import com.atmire.utils.MetadataUtils;
import com.atmire.utils.NullValidation;
import com.google.common.base.Function;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.lang.UnhandledException;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
                return Collections.singletonMap("workspaceID", workspaceID);
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
        Collections.addAll(fields, getConfigFields());
        return fields;
    }

    private String[] getConfigFields() {
        String property = ConfigurationManager.getProperty("clone.item.metadata.wipe");
        return (property == null ? "" : property).split(",");
    }

    private String getIdentifier(Item item) {
        return MetadataUtils.getMetadataFirstValue(item, "vlaanderen.identifier");
    }
}
