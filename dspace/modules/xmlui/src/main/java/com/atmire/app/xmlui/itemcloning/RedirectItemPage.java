package com.atmire.app.xmlui.itemcloning;

import org.apache.cocoon.environment.http.HttpEnvironment;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.content.Item;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 19 Jun 2018
 */
public class RedirectItemPage extends AbstractDSpaceTransformer {

    @Override
    public void addBody(Body body) throws IOException {
        Item item = CloneItemUtils.getItem(context, objectModel);
        if (item != null) {
            ((HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT))
                    .sendRedirect(getItemURL(item));
        }
    }

    private String getItemURL(Item item) {
        return contextPath + "/handle/" + item.getHandle();
    }
}