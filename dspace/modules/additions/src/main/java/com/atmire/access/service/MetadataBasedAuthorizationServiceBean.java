package com.atmire.access.service;

import com.atmire.access.factory.MetdataBasedAccessControlPoliciesFactory;
import com.atmire.access.model.Policy;
import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.sql.SQLException;
import java.util.List;

/**
 * @author philip at atmire.com
 */
public class MetadataBasedAuthorizationServiceBean implements MetadataBasedAuthorizationService {

    private static Logger log = Logger.getLogger(MetadataBasedAuthorizationServiceBean.class);

    private MetdataBasedAccessControlPoliciesFactory metdataBasedAccessControlPoliciesFactory;

    @Override
    public boolean isAuthorized(Context context, EPerson eperson, Group group, DSpaceObject object) {
        try {
            if (object instanceof Item) {
                return isAuthorized(context, eperson, group, (Item) object);
            } else if (object instanceof Bitstream && object.getParentObject() instanceof Item) {
                return isAuthorized(context, eperson, group, (Item) object.getParentObject());
            }

        } catch (SQLException e) {
            log.warn("Unable to read bitstream information: " + e.getMessage(), e);
        }

        return true;
    }

    private boolean isAuthorized(Context context, EPerson eperson, Group group, Item item) {
        try {
            context.turnOffAuthorisationSystem();

            List<? extends Policy> policies = retrievePoliciesForGroup(group);

            for (Policy policy : policies) {
                if (!policy.isAuthorized(eperson, item)) {
                    return false;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            context.restoreAuthSystemState();
        }

        return true;
    }

    public List<Policy> retrievePoliciesForGroup(Group group) {
        return metdataBasedAccessControlPoliciesFactory.getPolicies(group.getName());
    }

    public void setMetdataBasedAccessControlPoliciesFactory(MetdataBasedAccessControlPoliciesFactory metdataBasedAccessControlPoliciesFactory) {
        this.metdataBasedAccessControlPoliciesFactory = metdataBasedAccessControlPoliciesFactory;
    }
}
