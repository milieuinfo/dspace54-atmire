package com.atmire.access.service;

import com.atmire.access.factory.*;
import com.atmire.access.model.*;
import java.util.*;
import org.apache.log4j.*;
import org.dspace.content.*;
import org.dspace.core.*;
import org.dspace.eperson.*;

/**
 * @author philip at atmire.com
 */
public class MetadataBasedAuthorizationServiceBean implements MetadataBasedAuthorizationService {

    private static Logger log = Logger.getLogger(MetadataBasedAuthorizationServiceBean.class);

    private MetdataBasedAccessControlPoliciesFactory metdataBasedAccessControlPoliciesFactory;

    @Override
    public boolean isAuthorized(Context context, EPerson eperson, Group group, Item item) {
        try {
            context.turnOffAuthorisationSystem();

            List<? extends Policy> policies = metdataBasedAccessControlPoliciesFactory.getPolicies(group.getName());

            for (Policy policy : policies) {
                if (!policy.isAuthorized(eperson, item)) {
                    return false;
                }
            }
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
        finally {
            context.restoreAuthSystemState();
        }

        return true;
    }

    public void setMetdataBasedAccessControlPoliciesFactory(MetdataBasedAccessControlPoliciesFactory metdataBasedAccessControlPoliciesFactory) {
        this.metdataBasedAccessControlPoliciesFactory = metdataBasedAccessControlPoliciesFactory;
    }
}
