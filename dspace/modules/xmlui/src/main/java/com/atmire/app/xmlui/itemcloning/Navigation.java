package com.atmire.app.xmlui.itemcloning;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 19 Jun 2018
 */
public class Navigation extends AbstractDSpaceTransformer {
    private static final Message T_clone_item = message
            ("xmlui.itemcloning.CloneItemTransfomer.sidebar");

    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException {
        /* Create skeleton menu structure to ensure consistent order between aspects,
         * even if they are never used
         */
        options.addList("browse");
        List account = options.addList("account");
        List context = options.addList("context");
        List admin = options.addList("administrative");

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (dso instanceof Item) {
            Item item = (Item) dso;
            if (CloneItemUtils.authorizeCloning(this.context, item)) {
                context.addItem()
                        .addXref(contextPath + "/clone-item?itemID=" + item.getID(), T_clone_item);
            }
        }
    }
}
