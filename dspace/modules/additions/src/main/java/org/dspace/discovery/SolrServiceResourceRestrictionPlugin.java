/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import com.atmire.access.model.Policy;
import com.atmire.access.service.MetadataBasedAuthorizationService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Restriction plugin that ensures that indexes all the resource policies.
 * When a search is performed extra filter queries are added to retrieve only results to which the user has READ access
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class SolrServiceResourceRestrictionPlugin implements SolrServiceIndexPlugin, SolrServiceSearchPlugin {

    private static final Logger log = Logger.getLogger(SolrServiceResourceRestrictionPlugin.class);

    @Override
    public void additionalIndex(Context context, DSpaceObject dso, SolrInputDocument document) {
        try {
            List<ResourcePolicy> policies = AuthorizeManager.getPoliciesActionFilter(context, dso, Constants.READ);
            for (ResourcePolicy resourcePolicy : policies) {
                String fieldValue;
                if (resourcePolicy.getGroupID() != -1) {
                    //We have a group add it to the value
                    fieldValue = "g" + resourcePolicy.getGroupID();
                } else {
                    //We have an eperson add it to the value
                    fieldValue = "e" + resourcePolicy.getEPersonID();
                }

                document.addField("read", fieldValue);
            }
        } catch (SQLException e) {
            log.error(LogManager.getHeader(context, "Error while indexing resource policies", "DSpace object: (id " + dso.getID() + " type " + dso.getType() + ")"));
        }
    }

    @Override
    public void additionalSearchParameters(Context context, DiscoverQuery discoveryQuery, SolrQuery solrQuery) {
        try {
            if (!AuthorizeManager.isAdmin(context)) {
                StringBuilder resourceQuery = new StringBuilder();
                //Always add the anonymous group id to the query
                resourceQuery.append("(read:g0)");
                EPerson currentUser = context.getCurrentUser();
                if (currentUser != null) {
                    resourceQuery.append(" OR (read:e").append(currentUser.getID()).append(")");
                }

                //Retrieve all the groups the current user is a member of (including parent groups)!
                Set<Integer> groupIds = Group.allMemberGroupIDs(context, currentUser);

                List<MetadataBasedAuthorizationService> metadataBasedAuthorizationServiceList =
                        new DSpace().getServiceManager().getServicesByType(MetadataBasedAuthorizationService.class);

                //If we have metadata based access policies enabled
                if (metadataBasedAuthorizationServiceList.size() > 0) {
                    MetadataBasedAuthorizationService metadataBasedAuthorizationService = metadataBasedAuthorizationServiceList.get(0);

                    for (Integer groupId : groupIds) {
                        //build restrictions for groups without metadata based policies
                        Group group = Group.find(context, groupId);

                        //If the current group has metadata based restriction policies
                        List<Policy> policiesFromGroup = retrievePoliciesForGroupAndMembers(context, metadataBasedAuthorizationService, group);

                        StringBuilder criteria = new StringBuilder();
                        if (CollectionUtils.isNotEmpty(policiesFromGroup)) {
                            //For each of the policies of the current group or one of his subgroups, make sure one of the policies is fulfilled
                            for (Policy policy : policiesFromGroup) {
                                if(StringUtils.isNotBlank(criteria)) {
                                    criteria.append(" OR ");
                                }
                                String solrQueryCriteria = policy.getSolrQueryCriteria(currentUser);

                                criteria.append("(read:g" + group.getID());
                                if (StringUtils.isNotBlank(solrQueryCriteria)) {
                                    criteria.append(" AND " + solrQueryCriteria);
                                }
                                criteria.append(")");
                            }
                        } else {
                            criteria.append("(read:g").append(groupId).append(")");
                        }

                        //Add them as extra search criteria
                        resourceQuery.append(" OR " + criteria.toString());
                    }
                } else {
                    for (Integer groupId : groupIds) {
                        resourceQuery.append(" OR (read:g").append(groupId).append(")");
                    }
                }

                solrQuery.addFilterQuery(resourceQuery.toString());

            }
        } catch (SQLException e) {
            log.error(LogManager.getHeader(context, "Error while adding resource policy information to query", ""), e);
        }
    }

    private List<Policy> retrievePoliciesForGroupAndMembers(Context context, MetadataBasedAuthorizationService metadataBasedAuthorizationService, Group group) throws SQLException {
        EPerson currentUser = context.getCurrentUser();
        if (currentUser != null) {
            List<Group> groupsToCheck = Group.allMemberGroups(context, group);
            groupsToCheck.add(group);

            List<Policy> policiesFromGroup = new LinkedList<>();

            for (Group memberGroup : groupsToCheck) {
                if(context.inSpecialGroup(memberGroup.getID()) || memberGroup.isMember(currentUser)) {
                    policiesFromGroup.addAll(metadataBasedAuthorizationService.retrievePoliciesForGroup(memberGroup));
                }
            }

            return policiesFromGroup;
        } else {
            return Collections.emptyList();
        }
    }

}
