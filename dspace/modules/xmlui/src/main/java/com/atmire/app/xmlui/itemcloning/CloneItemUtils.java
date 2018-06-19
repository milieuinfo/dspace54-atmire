package com.atmire.app.xmlui.itemcloning;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.UnhandledException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.Map;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 19 Jun 2018
 */
public class CloneItemUtils {

    private static final Logger log = LogManager.getLogger(CloneItemUtils.class);

    public static Item getItem(Context context, Map objectModel) {
        try {
            Request request = ObjectModelHelper.getRequest(objectModel);
            String itemID = request.getParameter("itemID");
            if (StringUtils.isBlank(itemID)) {
                return null;
            } else {
                return Item.find(context, Integer.parseInt(itemID));
            }
        } catch (NumberFormatException | SQLException e) {
            throw new UnhandledException(e);
        }
    }

    public static boolean authorizeCloning(Map objectModel) {
        try {
            Context context = ContextUtil.obtainContext(objectModel);
            Item item = CloneItemUtils.getItem(context, objectModel);
            return authorizeCloning(context, item);
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public static boolean authorizeCloning(Context context, Item item) {
        try {
            if (item != null) {
                Collection owningCollection = item.getOwningCollection();
                if (owningCollection != null) {
                    return AuthorizeManager.authorizeActionBoolean
                            (context, owningCollection, Constants.ADD);
                }
            }
            return false;
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

}
