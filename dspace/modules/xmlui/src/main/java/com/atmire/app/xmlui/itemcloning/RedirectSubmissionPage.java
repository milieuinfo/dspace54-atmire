package com.atmire.app.xmlui.itemcloning;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.commons.lang.UnhandledException;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.element.Body;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 19 Jun 2018
 */
public class RedirectSubmissionPage extends AbstractDSpaceTransformer {

    @Override
    public void addBody(Body body) throws IOException {
        try {
            String workspaceID = parameters.getParameter("workspaceID");
            String handle = parameters.getParameter("handle");
            ((HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT))
                    .sendRedirect(getItemURL(handle, workspaceID));
        } catch (ParameterException e) {
            throw new UnhandledException(e);
        }
    }

    private String getItemURL(String handle, String workspaceID) {
        return contextPath + "/handle/" + handle + "/submit-clone?workspaceID=" + workspaceID;
    }

}