package com.atmire.app.xmlui.aspect.compliance;

import com.atmire.authorization.AuthorizationChecker;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.selection.Selector;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.general.AuthenticatedSelector;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.Map;

/**
 * Created by jonas - jonas@atmire.com on 13/04/16.
 */
public class ComplianceAuthorizedSelector extends AuthenticatedSelector implements Selector {

    /* Log4j logger*/
    private static final Logger log = Logger.getLogger(ComplianceAuthorizedSelector.class);


    public boolean select(
            String expression, Map objectModel,
            Parameters parameters
    ) {
        boolean authenticatedSelect = super.select(expression, objectModel, parameters);
        if (!authenticatedSelect) {
            return authenticatedSelect;
        }
        Context context;
        boolean authorized = false;
        try {
            context = ContextUtil.obtainContext(objectModel);

            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            authorized = authorizedForCompliance(context, dso);

        } catch (SQLException e) {
            log.error(e);
        }

        return authenticatedSelect && authorized;
    }

    public boolean authorizedForCompliance(Context context, DSpaceObject dso) {
        return new DSpace().getServiceManager()
                .getServiceByName("ComplianceAuthorizationChecker", AuthorizationChecker.class)
                .checkAuthorization(context, dso);
    }
}
