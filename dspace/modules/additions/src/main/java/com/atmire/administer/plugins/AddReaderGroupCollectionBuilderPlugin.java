package com.atmire.administer.plugins;

import java.sql.*;
import org.dspace.authorize.*;
import org.dspace.content.*;
import org.dspace.core.*;
import org.dspace.eperson.*;
import org.w3c.dom.*;

/**
 * @author philip at atmire.com
 */
public class AddReaderGroupCollectionBuilderPlugin extends CollectionBuilderPlugin {
    @Override
    public void updateCollection(Context context, Collection collection, NodeList values) throws SQLException, AuthorizeException {
        Group readers = createCollectionDefaultReadGroup(context,collection);
        addValuesToGroup(context, readers, values);
    }

    private Group createCollectionDefaultReadGroup(Context context, Collection collection) throws SQLException, AuthorizeException
    {
        Group role = null;
        int roleID = getCollectionDefaultRead(context, collection);

        if (roleID == 0)
        {
            role = Group.create(context);
            role.setName("COLLECTION_"+collection.getID() +"_DEFAULT_READ");

            // Remove existing privileges from the anonymous group.
            AuthorizeManager.removePoliciesActionFilter(context, collection, Constants.DEFAULT_ITEM_READ);
            AuthorizeManager.removePoliciesActionFilter(context, collection, Constants.DEFAULT_BITSTREAM_READ);

            // Grant our new role the default privileges.
            AuthorizeManager.addPolicy(context, collection, Constants.DEFAULT_ITEM_READ,      role);
            AuthorizeManager.addPolicy(context, collection, Constants.DEFAULT_BITSTREAM_READ, role);

            // Commit the changes
            role.update();
            context.commit();
        }
        else {
            role = Group.find(context, roleID);
        }

        return role;
    }

    private int getCollectionDefaultRead(Context context, Collection collection) throws SQLException, AuthorizeException
    {
        Group[] itemGroups = AuthorizeManager.getAuthorizedGroups(context, collection, Constants.DEFAULT_ITEM_READ);
        Group[] bitstreamGroups = AuthorizeManager.getAuthorizedGroups(context, collection, Constants.DEFAULT_BITSTREAM_READ);

        int itemGroupID = -1;

        // If there are more than one groups assigned either of these privileges then this role based method will not work.
        // The user will need to go to the authorization section to manually straighten this out.
        if (itemGroups.length != 1 || bitstreamGroups.length != 1)
        {
            // do nothing the itemGroupID is already set to -1
        }
        else
        {
            Group itemGroup = itemGroups[0];
            Group bitstreamGroup = bitstreamGroups[0];

            // If the same group is not assigned both of these privileges then this role based method will not work. The user
            // will need to go to the authorization section to manually straighten this out.
            if (itemGroup.getID() != bitstreamGroup.getID())
            {
                // do nothing the itemGroupID is already set to -1
            }
            else
            {
                itemGroupID = itemGroup.getID();
            }
        }

        return itemGroupID;
    }
}
