package com.atmire.administer.plugins;

import java.sql.*;
import org.apache.log4j.*;
import org.dspace.authorize.*;
import org.dspace.content.*;
import org.dspace.core.*;
import org.dspace.eperson.*;
import org.dspace.xmlworkflow.*;
import org.w3c.dom.*;

/**
 * @author philip at atmire.com
 */
public class AddWorkflowGroupCollectionBuilderPlugin extends CollectionBuilderPlugin {

    private static Logger log = Logger.getLogger(AddWorkflowGroupCollectionBuilderPlugin.class);

    private String roleName;

    @Override
    public void updateCollection(Context context, Collection collection, NodeList values) throws SQLException, AuthorizeException {
        Group reviewers = null;
        try {
            Role role = WorkflowUtils.getCollectionAndRepositoryRoles(collection).get(roleName);
            reviewers = WorkflowUtils.getRoleGroup(context, collection.getID(), role);

            if(reviewers == null) {
                reviewers = Group.create(context);
                reviewers.setName("COLLECTION_" + collection.getID() + "_WORKFLOW_ROLE_" + roleName);
                reviewers.update();

                AuthorizeManager.addPolicy(context, collection, Constants.ADD, reviewers);
                WorkflowUtils.createCollectionWorkflowRole(context, collection.getID(), roleName, reviewers);
                collection.update();
                context.commit();
            }

        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }

        addValuesToGroup(context, reviewers, values);
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
