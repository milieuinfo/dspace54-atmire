/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.handle.HandleManager;
import org.dspace.usage.UsageEvent;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/search")
@Api(value = "/search", description = "Search for objects", position = 2)

public class SearchResource extends Resource {


    /** log4j category */
    private static final Logger log = Logger.getLogger(SearchResource.class);

    private org.dspace.core.Context context;

    /**
     * Limits the search to this collection / community.
     */
    private DSpaceObject scopeObject;

    public SearchResource()
    {
    }

    @GET
    @Path("/item")
    @ApiOperation(value = "Retrieve items based on given pairs of metadatafields and values.",
            response = Item.class
    )
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response searchItems(

            @ApiParam(value = "The metadatafields to use in the search, colon (;) separated, in the form of \"metadatafield:value\"", required = true)
            @QueryParam("fields") String fields,

            @ApiParam(value = "Show additional data for the item.", required = false, allowMultiple = true, allowableValues = "all,metadata,parentCollection,parentCollectionList,parentCommunityList,bitstreams")
            @QueryParam("expand") String expand,

            @ApiParam(value = "The maximum amount of items shown.", required = false)
            @QueryParam("limit") int limit,
            @ApiParam(value = "The amount of items to skip.", required = false)
            @QueryParam("offset") int offset,
            @ApiParam(value = "The field by which the result should be sorted.", required = false)
            @QueryParam("sort-by") String sortBy,
            @ApiParam(value = "A field to limit the search to certain collections/communities.", required = false)
            @QueryParam("scope") String scope,
            @ApiParam(value = "The ordering of the results.", required = false, allowableValues = "asc,desc")
            @QueryParam("order") String order,

            @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException, Exception {

        // get the context user.
        context = createContext();

        if(log.isDebugEnabled())
        {
            log.debug("user:  " + context.getCurrentUser());
        }

        DiscoverQuery dq = new DiscoverQuery();

        dq.setDSpaceObjectFilter(org.dspace.core.Constants.ITEM);

        String cleanFields = StringUtils.trimToNull(fields);
        String cleanExpand = StringUtils.trimToNull(expand);

        if (null == cleanExpand) {
            cleanExpand = "metadata";
        }

        if (null != cleanFields) {
            String[] splitFields = StringUtils.split(cleanFields, ';');
            String query = StringUtils.join(splitFields, " AND ");
            dq.setQuery(query);
        }

        dq.setMaxResults(getMaxResults(limit));

        dq.setStart(offset);


        Response response;
        try {
            if (StringUtils.isNotBlank(scope)) {
                scopeObject = getScope(scope);
            }

            SearchService searchService = SearchUtils.getSearchService();

            if (sortBy != null) {
                String field = getSortFieldName(sortBy, scopeObject);
                dq.setSortField(field, getSortOrder(order));
            }

            //DSpaceObject dso = Collection.find(context);
            DiscoverResult result = scopeObject == null ? searchService.search(context, dq) : searchService.search(context, scopeObject, dq);


            List<DSpaceObject> dspaceObjects = result.getDspaceObjects();

            List<org.dspace.rest.common.Item> toReturn = new ArrayList<org.dspace.rest.common.Item>();
            for (DSpaceObject obj : dspaceObjects) {
                Item it = (Item) obj;
                toReturn.add(new org.dspace.rest.common.Item(it, cleanExpand, context));

                writeStats(it, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor,
                        headers, request, context);
            }

            GenericEntity<List<org.dspace.rest.common.Item>> entity = new GenericEntity<List<org.dspace.rest.common.Item>>(toReturn) {
            };

            response = Response.ok(entity).build();

        } catch (Exception ex) {
            log.error(ex.getMessage());
            response = Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        } finally {
            context.complete();
        }

        return response;
    }



    /**
     * When provided with a handle, this method will return the collection / community on which we need to filter.
     *
     * @param handle
     * @return
     * @throws Exception Throws a generic exception when the handle was not found.
     */
    private DSpaceObject getScope(String handle) throws Exception // throw exception on invalid scope.
    {
        DSpaceObject dso = HandleManager.resolveToObject(context, handle);
        if (dso == null) {
            throw new Exception("handle not found");
        }
        return dso;
    }

    private int getMaxResults(int limit) throws IllegalArgumentException {
        if (limit < 0) {
            throw new IllegalArgumentException("Limit can't be negative");
        }
        return limit == 0 ? 10 : limit;
    }

    private DiscoverQuery.SORT_ORDER getSortOrder(String order) {
        return order == null ? DiscoverQuery.SORT_ORDER.asc : DiscoverQuery.SORT_ORDER.valueOf(order);
    }

    private String getSortFieldName(String sortBy, DSpaceObject dso) {
        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(dso);

        Map<String, DiscoverySortFieldConfiguration> sortFields = new HashMap<String, DiscoverySortFieldConfiguration>();
        DiscoverySortConfiguration sortConfiguration = discoveryConfiguration.getSearchSortConfiguration();
        if(sortConfiguration != null)
        {
            for (DiscoverySortFieldConfiguration discoverySortConfiguration : sortConfiguration.getSortFields())
            {
                if (sortBy.equals(discoverySortConfiguration.getMetadataField())) {
                    if (discoverySortConfiguration.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)) {
                        return sortBy + "_dt";
                    }
                    return sortBy + "_sort";
                }
            }
        }
        return sortBy;
    }
}