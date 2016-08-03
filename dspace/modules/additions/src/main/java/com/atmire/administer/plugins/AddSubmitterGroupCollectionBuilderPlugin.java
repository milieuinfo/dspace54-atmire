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
public class AddSubmitterGroupCollectionBuilderPlugin extends CollectionBuilderPlugin {
    @Override
    public void updateCollection(Context context, Collection collection, NodeList values) throws SQLException, AuthorizeException {
        Group submitters = collection.createSubmitters();
        addValuesToGroup(context, submitters, values);
    }
}
