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
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.browse.BrowseException;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.LogManager;
import org.dspace.rest.common.Collection;
import org.dspace.rest.common.Item;
import org.dspace.rest.common.MetadataEntry;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.usage.UsageEvent;
import org.dspace.workflow.WorkflowManager;
import org.dspace.xmlworkflow.XmlWorkflowManager;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides all CRUD operation over collections.
 *
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 */
@Path("/collections")
@Api(value = "/collections", description = "Retrieve collections", position = 6)
public class CollectionsResource extends Resource
{
    private static Logger log = Logger.getLogger(CollectionsResource.class);

    /**
     * Return instance of collection with passed id. You can add more properties
     * through expand parameter.
     *
     * @param collectionId
     *            Id of collection in DSpace.
     * @param expand
     *            String in which is what you want to add to returned instance
     *            of collection. Options are: "all", "parentCommunityList",
     *            "parentCommunity", "items", "license" and "logo". If you want
     *            to use multiple options, it must be separated by commas.
     * @param limit
     *            Limit value for items in list in collection. Default value is
     *            100.
     * @param offset
     *            Offset of start index in list of items of collection. Default
     *            value is 0.
     * @param headers
     *            If you want to access to collection under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return instance of collection. It can also return status code
     *         NOT_FOUND(404) if id of collection is incorrect or status code
     *         UNATHORIZED(401) if user has no permission to read collection.
     * @throws WebApplicationException
     *             It is thrown when was problem with database reading
     *             (SQLException) or problem with creating
     *             context(ContextException). It is thrown by NOT_FOUND and
     *             UNATHORIZED status codes, too.
     */
    @GET
    @Path("/{collection_id}")
    @ApiOperation(value = "Retrieve a single collection by using the internal DSpace collection identifier.",
            response = org.dspace.rest.common.Collection.class
    )
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Collection getCollection(
            @ApiParam( value = "The identifier of the collection.", required = true )
            @PathParam("collection_id") Integer collectionId,

            @ApiParam( value = "Show additional data for the collection.", required = false, allowMultiple = true, allowableValues = "all,items,parentCommunity,parentCommunityList,license,logo")
            @QueryParam("expand") String expand,

            @ApiParam( value = "The maximum amount of collections shown.", required = false)
            @QueryParam("limit") @DefaultValue("100") Integer limit,

            @ApiParam( value = "The amount of collections to skip.", required = false)
            @QueryParam("offset") @DefaultValue("0") Integer offset,

            @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Reading collection(id=" + collectionId + ").");
        org.dspace.core.Context context = null;
        Collection collection = null;

        try
        {
            context = createContext();

            org.dspace.content.Collection dspaceCollection = findCollection(context, collectionId, org.dspace.core.Constants.READ);
            writeStats(dspaceCollection, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor,
                    headers, request, context);

            collection = new Collection(dspaceCollection, expand, context, limit, offset);
            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not read collection(id=" + collectionId + "), SQLException. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not read collection(id=" + collectionId + "), ContextException. Message: " + e.getMessage(),
                    context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("Collection(id=" + collectionId + ") has been successfully read.");
        return collection;
    }

    /**
     * Return array of all collections in DSpace. You can add more properties
     * through expand parameter.
     *
     * @param expand
     *            String in which is what you want to add to returned instance
     *            of collection. Options are: "all", "parentCommunityList",
     *            "parentCommunity", "items", "license" and "logo". If you want
     *            to use multiple options, it must be separated by commas.
     * @param limit
     *            Limit value for items in list in collection. Default value is
     *            100.
     * @param offset
     *            Offset of start index in list of items of collection. Default
     *            value is 0.
     * @param headers
     *            If you want to access to collections under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return array of collection, on which has logged user permission
     *         to view.
     * @throws WebApplicationException
     *             It is thrown when was problem with database reading
     *             (SQLException) or problem with creating
     *             context(ContextException).
     */
    @GET
    @ApiOperation(value = "Retrieve a list of collections.",
            response = org.dspace.rest.common.Collection[].class
    )
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Collection[] getCollections(
            @ApiParam( value = "Show additional data for the collection.", required = false, allowMultiple = true, allowableValues = "all,items,parentCommunity,parentCommunityList,license,logo")
            @QueryParam("expand") String expand,

            @ApiParam( value = "The maximum amount of collections shown.", required = false)
            @QueryParam("limit") @DefaultValue("100") Integer limit,

            @ApiParam( value = "The amount of collections to skip.", required = false)
            @QueryParam("offset") @DefaultValue("0") Integer offset,

            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Reading all collections.(offset=" + offset + ",limit=" + limit + ")");
        org.dspace.core.Context context = null;
        List<Collection> collections = new ArrayList<Collection>();

        try
        {
            context = createContext();

            if (!((limit != null) && (limit >= 0) && (offset != null) && (offset >= 0)))
            {
                log.warn("Paging was badly set.");
                limit = 100;
                offset = 0;
            }

            org.dspace.content.Collection[] dspaceCollections = org.dspace.content.Collection.findAll(context, limit, offset);
            for(org.dspace.content.Collection dspaceCollection : dspaceCollections)
            {
                if (AuthorizeManager.authorizeActionBoolean(context, dspaceCollection, org.dspace.core.Constants.READ))
                {
                    Collection collection = new Collection(dspaceCollection, null, context, limit,
                            offset);
                    collections.add(collection);
                    writeStats(dspaceCollection, UsageEvent.Action.VIEW, user_ip, user_agent,
                            xforwardedfor, headers, request, context);
                }
            }
            context.complete();
        }
        catch (SQLException e)
        {
            processException("Something went wrong while reading collections from database. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Something went wrong while reading collections, ContextError. Message: " + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("All collections were successfully read.");
        return collections.toArray(new Collection[0]);
    }

    /**
     * Return array of items in collection. You can add more properties to items
     * with expand parameter.
     *
     * @param collectionId
     *            Id of collection in DSpace.
     * @param expand
     *            String which define, what additional properties will be in
     *            returned item. Options are separeted by commas and are: "all",
     *            "metadata", "parentCollection", "parentCollectionList",
     *            "parentCommunityList" and "bitstreams".
     * @param limit
     *            Limit value for items in array. Default value is 100.
     * @param offset
     *            Offset of start index in array of items of collection. Default
     *            value is 0.
     * @param headers
     *            If you want to access to collection under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return array of items, on which has logged user permission to
     *         read. It can also return status code NOT_FOUND(404) if id of
     *         collection is incorrect or status code UNATHORIZED(401) if user
     *         has no permission to read collection.
     * @throws WebApplicationException
     *             It is thrown when was problem with database reading
     *             (SQLException) or problem with creating
     *             context(ContextException). It is thrown by NOT_FOUND and
     *             UNATHORIZED status codes, too.
     */
    @GET
    @Path("/{collection_id}/items")
    @ApiOperation(value = "Retrieve a list of items in a collection by using the internal DSpace collection identifier.",
            response = org.dspace.rest.common.Item[].class
    )
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Item[] getCollectionItems(
            @ApiParam( value = "The identifier of the collection.", required = true )
            @PathParam("collection_id") Integer collectionId,

            @ApiParam( value = "Show additional data for the items.", required = false, allowMultiple = true, allowableValues = "all,metadata,parentCollection,parentCollectionList,parentCommunityList,bitstreams")
            @QueryParam("expand") String expand,

            @ApiParam( value = "The maximum amount of items shown.", required = false)
            @QueryParam("limit") @DefaultValue("100") Integer limit,

            @ApiParam( value = "The amount of items to skip.", required = false)
            @QueryParam("offset") @DefaultValue("0") Integer offset, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Reading collection(id=" + collectionId + ") items.");
        org.dspace.core.Context context = null;
        List<Item> items = null;

        try
        {
            context = createContext();

            org.dspace.content.Collection dspaceCollection = findCollection(context, collectionId, org.dspace.core.Constants.READ);
            writeStats(dspaceCollection, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor,
                    headers, request, context);

            items = new ArrayList<Item>();
            org.dspace.content.ItemIterator dspaceItems = dspaceCollection.getItems();
            for (int i = 0; (dspaceItems.hasNext()) && (i < (limit + offset)); i++)
            {
                if (i >= offset)
                {
                    org.dspace.content.Item dspaceItem = dspaceItems.next();
                    if (ItemService.isItemListedForUser(context, dspaceItem))
                    {
                        items.add(new Item(dspaceItem, expand, context));
                        writeStats(dspaceItem, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor,
                                headers, request, context);
                    }
                } else {
                    //Advance the iterator to offset.
                    dspaceItems.nextID();
                }
            }

            context.complete();
        }
        catch (SQLException e)
        {
            processException("Could not read collection items, SQLException. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Could not read collection items, ContextException. Message: " + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        log.trace("All items in collection(id=" + collectionId + ") were successfully read.");
        return items.toArray(new Item[0]);
    }

    /**
     * Create item in collection. Item can be without filled metadata.
     *
     * @param collectionId
     *            Id of collection in which will be item created.
     * @param item
     *            Item filled only with metadata, other variables are ignored.
     * @param headers
     *            If you want to access to collection under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return status code with item. Return status (OK)200 if item was
     *         created. NOT_FOUND(404) if id of collection does not exists.
     *         UNAUTHORIZED(401) if user have not permission to write items in
     *         collection.
     * @throws WebApplicationException
     *             It is thrown when was problem with database reading or
     *             writing (SQLException) or problem with creating
     *             context(ContextException) or problem with authorization to
     *             collection or IOException or problem with index item into
     *             browse index. It is thrown by NOT_FOUND and UNATHORIZED
     *             status codes, too.
     *
     */
    @POST
    @Path("/{collection_id}/items")
    @ApiOperation(value = "Create an item in a collection by using the internal DSpace collection identifier.",
            response = org.dspace.rest.common.Item.class
    )
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Item addCollectionItem(
            @ApiParam( value = "The identifier of the collection.", required = true )
            @PathParam("collection_id") Integer collectionId,

            @ApiParam(value = "Item object", required = true)
            Item item,

            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Create item in collection(id=" + collectionId + ").");
        org.dspace.core.Context context = null;
        Item returnItem = null;

        try
        {
            context = createContext();
            org.dspace.content.Collection dspaceCollection = findCollection(context, collectionId,
                    org.dspace.core.Constants.WRITE);

            writeStats(dspaceCollection, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);

            log.trace("Creating item in collection(id=" + collectionId + ").");
            org.dspace.content.WorkspaceItem workspaceItem = org.dspace.content.WorkspaceItem.create(context, dspaceCollection,
                    false);
            org.dspace.content.Item dspaceItem = workspaceItem.getItem();

            log.trace("Adding metadata to item(id=" + dspaceItem.getID() + ").");
            if (item.getMetadata() != null)
            {
                for (MetadataEntry entry : item.getMetadata())
                {
                    String data[] = mySplit(entry.getKey());
                    dspaceItem.addMetadata(data[0], data[1], data[2], entry.getLanguage(), entry.getValue());
                }
            }
            workspaceItem.update();

            // Must insert the item into workflow
            if(ConfigurationManager.getProperty("workflow","workflow.framework").equals("xmlworkflow")){
                try{
                    XmlWorkflowManager.start(context, workspaceItem);
                }catch (Exception e){
                    log.error(LogManager.getHeader(context, "Error while starting xml workflow", "Item id: " + dspaceItem.getID()), e);
                    throw new ContextException("Error while starting xml workflow: Item id: " + dspaceItem.getID(),e);
                }
            }else{
                WorkflowManager.start(context, (WorkspaceItem)workspaceItem);
            }
            returnItem=new Item(workspaceItem.getItem(),"",context);

            context.complete();

        }
        catch (SQLException e)
        {
            processException("Could not add item into collection(id=" + collectionId + "), SQLException. Message: " + e, context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not add item into collection(id=" + collectionId + "), AuthorizeException. Message: " + e,
                    context);
        }
        catch (IOException e)
        {
            processException("Could not add item into collection(id=" + collectionId + "), IOException. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException(
                    "Could not add item into collection(id=" + collectionId + "), ContextException. Message: " + e.getMessage(),
                    context);
        }
        finally
        {
            processFinally(context);
        }

        log.info("Item successfully created in collection(id=" + collectionId + "). Item handle=" + returnItem.getHandle());
        return returnItem;
    }

    /**
     * Update collection. It replace all properties.
     *
     * @param collectionId
     *            Id of collection in DSpace.
     * @param collection
     *            Collection which will replace properties of actual collection.
     * @param headers
     *            If you want to access to collection under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return response 200 if was everything all right. Otherwise 400
     *         when id of community was incorrect or 401 if was problem with
     *         permission to write into collection.
     * @throws WebApplicationException
     *             It is thrown when was problem with database reading or
     *             writing. Or problem with authorization to collection. Or
     *             problem with creating context.
     */
    @PUT
    @Path("/{collection_id}")
    @ApiOperation(value = "Update a collection by using the internal DSpace collection identifier.",
            response = Response.class
    )
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response updateCollection(
            @ApiParam( value = "The identifier of the collection.", required = true )
            @PathParam("collection_id") Integer collectionId,

            @ApiParam(value = "Collection object", required = true)
            Collection collection,

            @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Updating collection(id=" + collectionId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();
            org.dspace.content.Collection dspaceCollection = findCollection(context, collectionId,
                    org.dspace.core.Constants.WRITE);

            writeStats(dspaceCollection, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);

            dspaceCollection.setMetadata("name", collection.getName());
            dspaceCollection.setLicense(collection.getLicense());
            // dspaceCollection.setLogo(collection.getLogo()); // TODO Add this option.
            dspaceCollection.setMetadata(org.dspace.content.Collection.COPYRIGHT_TEXT, collection.getCopyrightText());
            dspaceCollection.setMetadata(org.dspace.content.Collection.INTRODUCTORY_TEXT, collection.getIntroductoryText());
            dspaceCollection.setMetadata(org.dspace.content.Collection.SHORT_DESCRIPTION, collection.getShortDescription());
            dspaceCollection.setMetadata(org.dspace.content.Collection.SIDEBAR_TEXT, collection.getSidebarText());
            dspaceCollection.update();

            context.complete();

        }
        catch (ContextException e)
        {
            processException("Could not update collection(id=" + collectionId + "), ContextException. Message: " + e.getMessage(),
                    context);
        }
        catch (SQLException e)
        {
            processException("Could not update collection(id=" + collectionId + "), SQLException. Message: " + e, context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not update collection(id=" + collectionId + "), AuthorizeException. Message: " + e, context);
        }
        finally
        {
            processFinally(context);
        }

        log.info("Collection(id=" + collectionId + ") successfully updated.");
        return Response.ok().build();
    }

    /**
     * Delete collection.
     *
     * @param collectionId
     *            Id of collection which will be deleted.
     * @param headers
     *            If you want to access to collection under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return Return response code OK(200) if was everything all right.
     *         Otherwise return NOT_FOUND(404) if was id of community or
     *         collection incorrect. Or (UNAUTHORIZED)401 if was problem with
     *         permission to community or collection.
     * @throws WebApplicationException
     *             It is throw when was problem with creating context or problem
     *             with database reading or writing. Or problem with deleting
     *             collection caused by IOException or authorization.
     */
    @DELETE
    @Path("/{collection_id}")
    @ApiOperation(value = "Delete a collection by using the internal DSpace collection identifier.",
            response = Response.class
    )
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response deleteCollection(
            @ApiParam( value = "The identifier of the collection.", required = true )
            @PathParam("collection_id") Integer collectionId,

            @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Delete collection(id=" + collectionId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();
            org.dspace.content.Collection dspaceCollection = findCollection(context, collectionId,
                    org.dspace.core.Constants.DELETE);

            writeStats(dspaceCollection, UsageEvent.Action.REMOVE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);

            org.dspace.content.Community community = (org.dspace.content.Community) dspaceCollection.getParentObject();
            community.removeCollection(dspaceCollection);

            context.complete();

        }
        catch (ContextException e)
        {
            processException(
                    "Could not delete collection(id=" + collectionId + "), ContextExcpetion. Message: " + e.getMessage(), context);
        }
        catch (SQLException e)
        {
            processException("Could not delete collection(id=" + collectionId + "), SQLException. Message: " + e, context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not delete collection(id=" + collectionId + "), AuthorizeException. Message: " + e, context);
        }
        catch (IOException e)
        {
            processException("Could not delete collection(id=" + collectionId + "), IOException. Message: " + e, context);
        }
        finally {
            processFinally(context);
        }

        log.info("Collection(id=" + collectionId + ") was successfully deleted.");
        return Response.ok().build();
    }

    /**
     * Delete item in collection.
     *
     * @param collectionId
     *            Id of collection which will be deleted.
     *
     * @param itemId
     *            Id of item in colletion.
     * @return It returns status code: OK(200). NOT_FOUND(404) if item or
     *         collection was not found, UNAUTHORIZED(401) if user is not
     *         allowed to delete item or permission to write into collection.
     * @throws WebApplicationException
     *             It can be thrown by: SQLException, when was problem with
     *             database reading or writting. AuthorizeException, when was
     *             problem with authorization to item or collection.
     *             IOException, when was problem with removing item.
     *             ContextException, when was problem with creating context of
     *             DSpace.
     */
    @DELETE
    @Path("/{collection_id}/items/{item_id}")
    @ApiOperation(value = "Delete an item from a collection by using the internal DSpace collection and item identifiers.",
            response = Response.class
    )
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response deleteCollectionItem(
            @ApiParam( value = "The identifier of the collection.", required = true )
            @PathParam("collection_id") Integer collectionId,

            @ApiParam( value = "The identifier of the item.", required = true )
            @PathParam("item_id") Integer itemId,

            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException
    {

        log.info("Delete item(id=" + itemId + ") in collection(id=" + collectionId + ").");
        org.dspace.core.Context context = null;

        try
        {
            context = createContext();
            org.dspace.content.Collection dspaceCollection = findCollection(context, collectionId,
                    org.dspace.core.Constants.WRITE);

            org.dspace.content.Item item = null;
            org.dspace.content.ItemIterator dspaceItems = dspaceCollection.getItems();
            while (dspaceItems.hasNext())
            {
                org.dspace.content.Item dspaceItem = dspaceItems.next();
                if (dspaceItem.getID() == itemId)
                {
                    item = dspaceItem;
                }
            }

            if (item == null)
            {
                context.abort();
                log.warn("Item(id=" + itemId + ") was not found!");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            else if (!AuthorizeManager.authorizeActionBoolean(context, item, org.dspace.core.Constants.REMOVE))
            {
                context.abort();
                if (context.getCurrentUser() != null)
                {
                    log.error("User(" + context.getCurrentUser().getEmail() + ") has not permission to delete item!");
                }
                else
                {
                    log.error("User(anonymous) has not permission to delete item!");
                }
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

            writeStats(dspaceCollection, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor,
                    headers, request, context);
            writeStats(item, UsageEvent.Action.REMOVE, user_ip, user_agent, xforwardedfor, headers, request, context);

            dspaceCollection.removeItem(item);

            context.complete();

        }
        catch (ContextException e)
        {
            processException("Could not delete item(id=" + itemId + ") in collection(id=" + collectionId
                    + "), ContextException. Message: " + e.getMessage(), context);
        }
        catch (SQLException e)
        {
            processException("Could not delete item(id=" + itemId + ") in collection(id=" + collectionId
                    + "), SQLException. Message: " + e, context);
        }
        catch (AuthorizeException e)
        {
            processException("Could not delete item(id=" + itemId + ") in collection(id=" + collectionId
                    + "), AuthorizeException. Message: " + e, context);
        }
        catch (IOException e)
        {
            processException("Could not delete item(id=" + itemId + ") in collection(id=" + collectionId
                    + "), IOException. Message: " + e, context);
        }
        finally {
            processFinally(context);
        }

        log.info("Item(id=" + itemId + ") in collection(id=" + collectionId + ") was successfully deleted.");
        return Response.ok().build();
    }

    /**
     * Search for first collection with passed name.
     *
     * @param name
     *            Name of collection.
     * @param headers
     *            If you want to access to collection under logged user into
     *            context. In headers must be set header "rest-dspace-token"
     *            with passed token from login method.
     * @return It returns null if collection was not found. Otherwise returns
     *         first founded collection.
     * @throws WebApplicationException
     */
    @POST
    @Path("/find-collection")
    @ApiOperation(value = "Search for a collection by the collection name.",
            response = Collection.class
    )
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Collection findCollectionByName(String name, @Context HttpHeaders headers) throws WebApplicationException
    {
        log.info("Searching for first collection with name=" + name + ".");
        org.dspace.core.Context context = null;
        Collection collection = null;

        try
        {
            context = createContext();
            org.dspace.content.Collection[] dspaceCollections;

            dspaceCollections = org.dspace.content.Collection.findAll(context);

            for (org.dspace.content.Collection dspaceCollection : dspaceCollections)
            {
                if (AuthorizeManager.authorizeActionBoolean(context, dspaceCollection, org.dspace.core.Constants.READ))
                {
                    if (dspaceCollection.getName().equals(name))
                    {
                        collection = new Collection(dspaceCollection, "", context, 100, 0);
                        break;
                    }
                }
            }

            context.complete();

        }
        catch (SQLException e)
        {
            processException("Something went wrong while searching for collection(name=" + name + ") from database. Message: "
                    + e, context);
        }
        catch (ContextException e)
        {
            processException("Something went wrong while searching for collection(name=" + name + "), ContextError. Message: "
                    + e.getMessage(), context);
        }
        finally
        {
            processFinally(context);
        }

        if (collection == null)
        {
            log.info("Collection was not found.");
        }
        else
        {
            log.info("Collection was found with id(" + collection.getId() + ").");
        }
        return collection;
    }

    /**
     * Find collection from DSpace database. It is encapsulation of method
     * org.dspace.content.Collection.find with checking if item exist and if
     * user logged into context has permission to do passed action.
     *
     * @param context
     *            Context of actual logged user.
     * @param id
     *            Id of collection in DSpace.
     * @param action
     *            Constant from org.dspace.core.Constants.
     * @return It returns DSpace collection.
     * @throws WebApplicationException
     *             Is thrown when item with passed id is not exists and if user
     *             has no permission to do passed action.
     */
    private org.dspace.content.Collection findCollection(org.dspace.core.Context context, int id, int action)
            throws WebApplicationException
    {
        org.dspace.content.Collection collection = null;
        try
        {
            collection = org.dspace.content.Collection.find(context, id);

            if (collection == null)
            {
                context.abort();
                log.warn("Collection(id=" + id + ") was not found!");
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            else if (!AuthorizeManager.authorizeActionBoolean(context, collection, action))
            {
                context.abort();
                if (context.getCurrentUser() != null)
                {
                    log.error("User(" + context.getCurrentUser().getEmail() + ") has not permission to "
                            + getActionString(action) + " collection!");
                }
                else
                {
                    log.error("User(anonymous) has not permission to " + getActionString(action) + " collection!");
                }
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

        }
        catch (SQLException e)
        {
            processException("Something get wrong while finding collection(id=" + id + "). SQLException, Message: " + e, context);
        }
        return collection;
    }
}