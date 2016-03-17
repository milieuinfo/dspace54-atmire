/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("/search")
public class SearchResource extends Resource {

    /** log4j category */
    private static final Logger log = Logger.getLogger(SearchResource.class);

    @GET
    @Path("/item")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public List<org.dspace.rest.common.Item> searchItems(
            @QueryParam("fields") String fields,
            @QueryParam("expand") String expand,
            @QueryParam("limit") int limit,
            @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException, Exception {

        DiscoverQuery dq = new DiscoverQuery();

        dq.setDSpaceObjectFilter(org.dspace.core.Constants.ITEM);

        String cleanFields = StringUtils.trimToNull(fields);
        String cleanExpand = StringUtils.trimToNull(expand);

        if (null == cleanExpand){
            cleanExpand = "metadata";
        }

        if (null != cleanFields) {
            String[] splitFields = StringUtils.split(cleanFields, ';');
            String query = StringUtils.join(splitFields , " AND ");
            dq.setQuery(query);
        }

        if (0 == limit) {
            dq.setMaxResults(10);
        }else{
            dq.setMaxResults(limit);
        }

        SearchService searchService = SearchUtils.getSearchService();

        org.dspace.core.Context context = new org.dspace.core.Context();

        context.turnOffAuthorisationSystem();

        DiscoverResult result = searchService.search(context, dq);

        List<DSpaceObject> dspaceObjects = result.getDspaceObjects();

        List<org.dspace.rest.common.Item> toReturn = new ArrayList<org.dspace.rest.common.Item>();
        for (DSpaceObject obj : dspaceObjects){
            Item it = (Item) obj;
            toReturn.add(new org.dspace.rest.common.Item(it,cleanExpand,context));
        }

        context.complete();

        return toReturn;
    }

}