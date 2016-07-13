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
public abstract class CollectionBuilderPlugin {

    public abstract void updateCollection(Context context, Collection collection, NodeList values) throws SQLException, AuthorizeException;

    private Group findOrCreate(Context context, String groupName) throws SQLException, AuthorizeException {
        Group group = Group.findByName(context, groupName);

        if(group==null){
            group = Group.create(context);
            group.setName(groupName);
            group.update();
            context.commit();
        }

        return group;
    }

    void addValuesToGroup(Context context, Group group, NodeList values) throws SQLException, AuthorizeException {
        for (int i = 0; i < values.getLength(); i++) {
            Node node = values.item(i);

            if(node.hasChildNodes()) {
                Node firstChild = node.getFirstChild();

                if (firstChild.getNodeType() == Node.TEXT_NODE)
                {
                    String groupName = firstChild.getNodeValue().trim();
                    Group groupToAdd = findOrCreate(context, groupName);

                    group.addMember(groupToAdd);
                    group.update();
                    context.commit();
                }
            }
        }
    }
}
