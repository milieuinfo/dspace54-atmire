/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("/search")
@Api(value = "/search", description = "Search for objects", position = 6)

public class SearchResource extends Resource {


    /** log4j category */
    private static final Logger log = Logger.getLogger(SearchResource.class);

    @GET
    @Path("/item")
    @ApiOperation(value = "Retrieve items based on given pairs of metadatafields and values.",
            response = Item.class
    )
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public List<org.dspace.rest.common.Item> searchItems(

            @ApiParam( value = "The metadatafields to use in the search, colon (;) separated, in the form of \"metadatafield:value\"", required = true)
            @QueryParam("fields") String fields,

            @ApiParam( value = "Show additional data for the item.", required = false, allowMultiple = true, allowableValues = "all,metadata,parentCollection,parentCollectionList,parentCommunityList,bitstreams")
            @QueryParam("expand") String expand,

            @ApiParam( value = "The maximum amount of items shown.", required = false)
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

    @GET
    @Path("/item2")
    @ApiOperation(value = "Retrieve items based on given pairs of metadatafields and values.",
            response = Item.class
    )
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public List<org.dspace.rest.common.Item> searchItems2(

            @ApiParam( value = "The metadatafields to use in the search, colon (;) separated, in the form of \"metadatafield:value\"", required = true)
            @QueryParam("fields") String fields,

            @ApiParam( value = "Show additional data for the item.", required = false, allowMultiple = true, allowableValues = "all,metadata,parentCollection,parentCollectionList,parentCommunityList,bitstreams")
            @QueryParam("expand") String expand,

            @ApiParam( value = "The maximum amount of items shown.", required = false)
            @QueryParam("limit") int limit,
            @QueryParam("offset") int offset,
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

        dq.setStart(offset);


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