package com.atmire.access.plugins;

import com.atmire.access.model.Policy;
import com.atmire.access.service.MetadataBasedAuthorizationService;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.eperson.Group;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by jonas - jonas@atmire.com on 26/07/16.
 */
public class MetadataBasedAccessControlIndexPlugin implements SolrServiceIndexPlugin {

    /* Log4j logger*/
    private static final Logger log = Logger.getLogger(MetadataBasedAccessControlIndexPlugin.class);

    @Override
    public void additionalIndex(Context context, DSpaceObject dso, SolrInputDocument document) {
        try {
            List<ResourcePolicy> policies = AuthorizeManager.getPoliciesActionFilter(context, dso, Constants.READ);

            List<MetadataBasedAuthorizationService> metadataBasedAuthorizationServiceList =
                    new DSpace().getServiceManager().getServicesByType(MetadataBasedAuthorizationService.class);

            if (metadataBasedAuthorizationServiceList.size() > 0 ) {
                MetadataBasedAuthorizationService metadataBasedAuthorizationService = metadataBasedAuthorizationServiceList.get(0);
                for (ResourcePolicy resourcePolicy: policies) {
                    List<Policy> policiesFromGroupAndMembers = retrievePoliciesFromGroupAndMembers(context, metadataBasedAuthorizationService, resourcePolicy);
                    for(Policy policy:policiesFromGroupAndMembers){
                        if(!document.containsKey(policy.getSolrIndexField())){
                            document.addField(policy.getSolrIndexField(),policy.getSolrIndexValue(context,dso));
                        }
                    }
                }
            }

        } catch (SQLException e) {
            log.error(LogManager.getHeader(context, "Error while indexing resource policies", "DSpace object: (id " + dso.getID() + " type " + dso.getType() + ")"));
        }
    }

    private List<Policy> retrievePoliciesFromGroupAndMembers(Context context, MetadataBasedAuthorizationService metadataBasedAuthorizationService, ResourcePolicy resourcePolicy) throws SQLException {
        int groupId= resourcePolicy.getGroupID();
        Group group = Group.find(context, groupId);

        List<Group> memberGroups = Group.allMemberGroups(context, group);

        List<Policy> policiesFromGroup = metadataBasedAuthorizationService.retrievePoliciesForGroup(group);

        for(Group memberGroup : memberGroups){
            policiesFromGroup.addAll(metadataBasedAuthorizationService.retrievePoliciesForGroup(memberGroup));
        }

        return policiesFromGroup;
    }
}
