package com.atmire.app.xmlui.aspect.compliance;

import com.atmire.authorization.AuthorizationChecker;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

/**
 * Created by jonas - jonas@atmire.com on 13/04/16.
 */
public class Navigation extends AbstractDSpaceTransformer implements CacheableProcessingComponent {

    private static final Message T_head_compliance =
            message("xmlui.Compliance.Navigation.head_compliance");

    private java.util.List<ComplianceUI> complianceUIs = new DSpace().getServiceManager()
            .getServicesByType(ComplianceUI.class);

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            Request request = ObjectModelHelper.getRequest(objectModel);
            String key
                    = request.getScheme() + request.getServerName() + request.getServerPort() + request
                    .getSitemapURI() + request.getQueryString();

            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            if (dso != null) {
                key += "-" + dso.getHandle();
            }

            return HashUtil.hash(key);
        } catch (SQLException sqle) {
            // Ignore all errors and just return that the component is not cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     * <p/>
     * The cache is always valid.
     */
    public SourceValidity getValidity() {
        return NOPValidity.SHARED_INSTANCE;
    }


    public void addOptions(Options options) throws SAXException, WingException,
            SQLException, IOException, AuthorizeException {
        /* Create skeleton menu structure to ensure consistent order between aspects,
         * even if they are never used
         */
        options.addList("browse");
        List compliance = options.addList("compliance");

        options.addList("account");
        options.addList("context");
        options.addList("administrative");
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        AuthorizationChecker complianceAuthorizationChecker = new DSpace().getServiceManager()
                .getServiceByName("ComplianceAuthorizationChecker", AuthorizationChecker.class);

        boolean isItemAndArchived = dso instanceof Item && ((Item) dso).isArchived();
        if (isItemAndArchived && complianceAuthorizationChecker.checkAuthorization(
                this.context,
                dso
        )) {
            compliance.setHead(T_head_compliance);

            for (ComplianceUI complianceUI : complianceUIs) {
                if (org.apache.commons.lang3.StringUtils.isNotBlank(complianceUI.getNavigationKey())) {
                    compliance.addItemXref(
                            contextPath + "/handle/" + dso.getHandle() + "/" + complianceUI.getIdentifier()
                                    + "-compliance",
                            new Message("default", complianceUI.getNavigationKey())
                    );
                }
            }
        }

    }

}


