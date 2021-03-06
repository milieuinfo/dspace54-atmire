/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import com.atmire.lne.content.ItemServiceBean;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Metadatum;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.Group;
import org.dspace.rest.common.Bitstream;
import org.dspace.rest.common.Item;
import org.dspace.rest.common.MetadataEntry;
import org.dspace.rest.exceptions.ContextException;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.usage.UsageEvent;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Class which provide all CRUD methods over items.
 *
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 */
// Every DSpace class used without namespace is from package org.dspace.rest.common.*. Otherwise namespace is defined.
@SuppressWarnings("deprecation")
@Path("/items")
@Api(value = "/items", description = "Retrieve items", position = 3)
public class ItemsResource extends Resource {

    private static final Logger log = Logger.getLogger(ItemsResource.class);

    /**
     * Return item properties without metadata and bitstreams. You can add
     * additional properties by parameter expand.
     *
     * @param itemId  Id of item in DSpace.
     * @param expand  String which define, what additional properties will be in
     *                returned item. Options are separeted by commas and are: "all",
     *                "metadata", "parentCollection", "parentCollectionList",
     *                "parentCommunityList" and "bitstreams".
     * @param headers If you want to access to item under logged user into context.
     *                In headers must be set header "rest-dspace-token" with passed
     *                token from login method.
     * @return If user is allowed to read item, it returns item. Otherwise is
     * thrown WebApplicationException with response status
     * UNAUTHORIZED(401) or NOT_FOUND(404) if was id incorrect.
     * @throws WebApplicationException This exception can be throw by NOT_FOUND(bad id of item),
     *                                 UNAUTHORIZED, SQLException if wasproblem with reading from
     *                                 database and ContextException, if there was problem with
     *                                 creating context of DSpace.
     */
    @GET
    @Path("/{item_id}")
    @ApiOperation(value = "Retrieve a single item by using the internal DSpace item identifier.",
            response = org.dspace.rest.common.Item.class
    )
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Item getItem(
            @ApiParam(value = "The identifier of the item.", required = true)
            @PathParam("item_id") Integer itemId,

            @ApiParam(value = "Show additional data for the item.", required = false, allowMultiple = true, allowableValues = "all,metadata,parentCollection,parentCollectionList,parentCommunityList,bitstreams")
            @QueryParam("expand") String expand,

            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException {

        log.info("Reading item(id=" + itemId + ").");
        org.dspace.core.Context context = null;
        Item item = null;

        try {
            context = createContext();
            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.READ);

            writeStats(dspaceItem, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor, headers, request, context);

            item = new Item(dspaceItem, expand, context);
            context.complete();
            log.trace("Item(id=" + itemId + ") was successfully read.");

        } catch (SQLException e) {
            processException("Could not read item(id=" + itemId + "), SQLException. Message: " + e, context);
        } catch (ContextException e) {
            processException("Could not read item(id=" + itemId + "), ContextException. Message: " + e.getMessage(), context);
        } finally {
            processFinally(context);
        }

        
        return item;
    }

    @GET
    @Path("/external-handle/{handle}")
    @ApiOperation(value = "Retrieve an item by using the external item handle.",
        response = org.dspace.rest.common.Item[].class
    )
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getItemByExternalHandle(
        @ApiParam(value = "The identifier of the item.", required = true)
        @PathParam("handle") String handle,

        @ApiParam(value = "Show additional data for the item.", required = false, allowMultiple = true, allowableValues = "all,metadata,parentCollection,parentCollectionList,parentCommunityList,bitstreams")
        @QueryParam("expand") String expand,

        @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
        @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
        throws WebApplicationException {

        org.dspace.core.Context context = null;

        try {

            context = createContext();
            List<org.dspace.content.Item> items = findItemsByExternalHandle(handle, context);

            Response response = null;
            if (CollectionUtils.isEmpty(items)) {
                response = Response.status(Status.NOT_FOUND).entity("Item with external handle " + handle + " was not found").build();
            } else {
                if (items.size() >= 1) {
                    Item[] returnItems = new Item[items.size()];
                    for (int i = 0; i < items.size(); i++) {
                        returnItems[i] = new Item(items.get(i), expand, context);
                        writeStats(items.get(i), UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor, headers, request, context);
                    }

                    if (items.size() > 1) {
                        response = Response.status(Status.CONFLICT).entity(returnItems).build();
                    } else {
                        response = Response.ok(returnItems).build();
                    }

                }
            }

            context.complete();
            return response;

        } catch (ContextException e) {
            processException("Could not read item(handle=" + handle + "), ContextException. Message: " + e, context);
        } catch (SearchServiceException e) {
            processException("Could not read item(handle=" + handle + "), SearchServiceException. Message: " + e, context);
        } catch (SQLException e) {
            processException("Could not read item(handle=" + handle + "), SQLException. Message: " + e, context);
        } catch (UnsupportedEncodingException e) {
            processException("Could not read item(handle=" + handle + "), UnsupportedEncodingException. Message: " + e, context);
        } finally {
            processFinally(context);
        }
        return null;
    }
    private List<org.dspace.content.Item> findItemsByExternalHandle(String externalHandle,org.dspace.core.Context context ) throws UnsupportedEncodingException,SearchServiceException{
        String dspaceHandle = URLDecoder.decode(externalHandle, "UTF-8");
        com.atmire.lne.content.ItemService service = new ItemServiceBean();
        return service.findItemsByExternalHandle(context, dspaceHandle);

    }

    @GET
    @Path("/external-handle/{handle}/bitstream")
    @ApiOperation(value = "Retrieve the bitstream associated with an item by using the external item handle.",
        response = javax.ws.rs.core.Response.class
    )
    public Response getBitstreamByExternalHandle(
        @ApiParam(value = "The identifier of the item.", required = true)
        @PathParam("handle") String handle,
        @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
        @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
        throws WebApplicationException {

        org.dspace.core.Context context = null;

        try {

            context = createContext();
            List<org.dspace.content.Item> items = findItemsByExternalHandle(handle, context);

            Response response = null;

            ImmutablePair<Status, String> error = returnErrorPage(items, handle);

            if (null != error ){
              response = Response.status(error.left).entity(error.right).build();

            }else {
              org.dspace.content.Bitstream bitstream = getBitstream(items);
              writeStats(bitstream, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor, headers,
                request, context);

              InputStream inputStream = bitstream.retrieve();
              String type = bitstream.getFormat().getMIMEType();
              String name = bitstream.getName();
              response = Response.ok(inputStream).type(type)
                  .header("Content-Disposition", "attachment; filename=\"" + name + "\"")
                  .build();

            }


            context.complete();
            return response;

        } catch (ContextException e) {
            processException("Could not read item(handle=" + handle + "), ContextException. Message: " + e, context);
        } catch (SearchServiceException e) {
            processException("Could not read item(handle=" + handle + "), SearchServiceException. Message: " + e, context);
        } catch (SQLException e) {
            processException("Could not read item(handle=" + handle + "), SQLException. Message: " + e, context);
        } catch (UnsupportedEncodingException e) {
            processException("Could not read item(handle=" + handle + "), UnsupportedEncodingException. Message: " + e, context);
        }catch (AuthorizeException e){
            processException("Could not retrieve file of bitstream, AuthorizeException! Message: " + e, context);
        }catch (IOException e){
            processException("Could not retrieve file of bitstream, AuthorizeException! Message: " + e, context);
        } finally {
            processFinally(context);
        }
        return null;
    }

    private ImmutablePair<Status,String> returnErrorPage(List<org.dspace.content.Item> items, String externalHandle) throws SQLException{
        if (items.isEmpty()){
            return new ImmutablePair<>(Status.NOT_FOUND, "No item found with external handle "+ externalHandle);
        }else if (items.size()!=1){
            return new ImmutablePair<>(Status.CONFLICT,"Too many items found for external handle "+ externalHandle);
        } else{
            org.dspace.content.Item item = items.get(0);
            return returnErrorPageBundles(item.getBundles(), externalHandle);
        }
    }


    private ImmutablePair<Status,String> returnErrorPageBundles(Bundle[] bundles, String externalHandle){

        if (bundles.length ==0){
            return new ImmutablePair<>(Status.NOT_FOUND, "No bundle associated to item with external handle "+externalHandle);
        }else {
            Bundle bundle = findOriginalBundle(bundles) ;

            if (null == bundle) {
                return new ImmutablePair<>(Status.CONFLICT,
                    "No bundle found that cointains the actual bitstream " + externalHandle);
            }else {
                return returnErrorPageBitstreams(bundle.getBitstreams(), externalHandle);
            }
        }
    }

    private Bundle findOriginalBundle(Bundle[] bundles){
        for (Bundle b : bundles){
            if (b.getName().equalsIgnoreCase("original")){
                return  b;
            }
        }
        return null;
    }

    private ImmutablePair<Status,String> returnErrorPageBitstreams(org.dspace.content.Bitstream[] bitstreams, String externalHandle){

        if (bitstreams.length == 0 ){
            return new ImmutablePair<>(Status.NOT_FOUND, "No bitstream associated to item with external handle "+externalHandle);
        } else if (bitstreams.length > 1) {
            return new ImmutablePair<>(Status.CONFLICT, "Too many bitstreams associated to item with external handle "+externalHandle);
        }else {
            // OK !!
            return null;
        }
    }

    private org.dspace.content.Bitstream getBitstream(List<org.dspace.content.Item> items) throws SQLException{
        return findOriginalBundle(items.get(0).getBundles()).getBitstreams()[0];
    }

    /**
     * It returns an array of items in DSpace. You can define how many items in
     * list will be and from which index will start. Items in list are sorted by
     * handle, not by id.
     *
     * @param limit   How many items in array will be. Default value is 100.
     * @param offset  On which index will array start. Default value is 0.
     * @param headers If you want to access to item under logged user into context.
     *                In headers must be set header "rest-dspace-token" with passed
     *                token from login method.
     * @return Return array of items, on which has logged user into context
     * permission.
     * @throws WebApplicationException It can be thrown by SQLException, when was problem with
     *                                 reading items from database or ContextException, when was
     *                                 problem with creating context of DSpace.
     */
    @GET
    @ApiOperation(value = "Retrieve all items from the repository.",
            response = org.dspace.rest.common.Item[].class
    )
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getItems(
            @ApiParam(value = "Show additional data for the item.", required = false, allowMultiple = true, allowableValues = "all,metadata,parentCollection,parentCollectionList,parentCommunityList,bitstreams")
            @QueryParam("expand") String expand,

            @ApiParam(value = "The maximum amount of items shown.", required = false)
            @QueryParam("limit") @DefaultValue("100") Integer limit,

            @ApiParam(value = "The amount of items to skip.", required = false)
            @QueryParam("offset") @DefaultValue("0") Integer offset,

            @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException {

        log.info("Reading items.(offset=" + offset + ",limit=" + limit + ").");

        try {
            SearchResource searchResource = new SearchResource();
            return searchResource.searchItems("*:*", expand, limit, offset, "dc.date.accessioned", null, "asc", user_ip, user_agent, xforwardedfor, headers, request);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        }

    }

    /**
     * Returns item metadata in list.
     *
     * @param itemId  Id of item in DSpace.
     * @param headers If you want to access to item under logged user into context.
     *                In headers must be set header "rest-dspace-token" with passed
     *                token from login method.
     * @return Return list of metadata fields if was everything ok. Otherwise it
     * throw WebApplication exception with response code NOT_FOUND(404)
     * or UNAUTHORIZED(401).
     * @throws WebApplicationException It can be thrown by two exceptions: SQLException if was
     *                                 problem wtih reading item from database and ContextException,
     *                                 if was problem with creating context of DSpace. And can be
     *                                 thrown by NOT_FOUND and UNAUTHORIZED too.
     */
    @GET
    @Path("/{item_id}/metadata")
    @ApiOperation(value = "Retrieve a list of metadata for an item by using the internal DSpace item identifier.",
            response = org.dspace.rest.common.MetadataEntry[].class
    )
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public MetadataEntry[] getItemMetadata(
            @ApiParam(value = "The identifier of the item.", required = true)
            @PathParam("item_id") Integer itemId,

            @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException {

        log.info("Reading item(id=" + itemId + ") metadata.");
        org.dspace.core.Context context = null;
        List<MetadataEntry> metadata = null;

        try {
            context = createContext();
            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.READ);

            writeStats(dspaceItem, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor, headers, request, context);

            metadata = new Item(dspaceItem, "metadata", context).getMetadata();
            context.complete();
        } catch (SQLException e) {
            processException("Could not read item(id=" + itemId + "), SQLException. Message: " + e, context);
        } catch (ContextException e) {
            processException("Could not read item(id=" + itemId + "), ContextException. Message: " + e.getMessage(), context);
        } finally {
            processFinally(context);
        }

        log.trace("Item(id=" + itemId + ") metadata were successfully read.");
        return metadata.toArray(new MetadataEntry[0]);
    }

    /**
     * Return array of bitstreams in item. It can be pagged.
     *
     * @param itemId  Id of item in DSpace.
     * @param limit   How many items will be in array.
     * @param offset  On which index will start array.
     * @param headers If you want to access to item under logged user into context.
     *                In headers must be set header "rest-dspace-token" with passed
     *                token from login method.
     * @return Return pagged array of bitstreams in item.
     * @throws WebApplicationException It can be throw by NOT_FOUND, UNAUTHORIZED, SQLException if
     *                                 was problem with reading from database and ContextException
     *                                 if was problem with creating context of DSpace.
     */
    @GET
    @Path("/{item_id}/bitstreams")
    @ApiOperation(value = "Retrieve a list of bitstreams for an item by using the internal DSpace item identifier.",
            response = org.dspace.rest.common.Bitstream[].class
    )
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Bitstream[] getItemBitstreams(
            @ApiParam(value = "The identifier of the item.", required = true)
            @PathParam("item_id") Integer itemId,

            @ApiParam(value = "The maximum amount of bitstreams shown.", required = false)
            @QueryParam("limit") @DefaultValue("20") Integer limit,

            @ApiParam(value = "The amount of bitstreams to skip.", required = false)
            @QueryParam("offset") @DefaultValue("0") Integer offset,

            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException {

        log.info("Reading item(id=" + itemId + ") bitstreams.(offset=" + offset + ",limit=" + limit + ")");
        org.dspace.core.Context context = null;
        List<Bitstream> bitstreams = null;
        try {
            context = createContext();
            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.READ);

            writeStats(dspaceItem, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor, headers, request, context);

            List<Bitstream> itemBitstreams = new Item(dspaceItem, "bitstreams", context).getBitstreams();

            if ((offset + limit) > (itemBitstreams.size() - offset)) {
                bitstreams = itemBitstreams.subList(offset, itemBitstreams.size());
            } else {
                bitstreams = itemBitstreams.subList(offset, offset + limit);
            }
            context.complete();
        } catch (SQLException e) {
            processException("Could not read item(id=" + itemId + ") bitstreams, SQLExcpetion. Message: " + e, context);
        } catch (ContextException e) {
            processException("Could not read item(id=" + itemId + ") bitstreams, ContextException. Message: " + e.getMessage(),
                    context);
        } finally {
            processFinally(context);
        }

        log.trace("Item(id=" + itemId + ") bitstreams were successfully read.");
        return bitstreams.toArray(new Bitstream[0]);
    }

    /**
     * Adding metadata fields to item. If metadata key is in item, it will be
     * added, NOT REPLACED!
     *
     * @param itemId   Id of item in DSpace.
     * @param metadata List of metadata fields, which will be added into item.
     * @param headers  If you want to access to item under logged user into context.
     *                 In headers must be set header "rest-dspace-token" with passed
     *                 token from login method.
     * @return It returns status code OK(200) if all was ok. UNAUTHORIZED(401)
     * if user is not allowed to write to item. NOT_FOUND(404) if id of
     * item is incorrect.
     * @throws WebApplicationException It is throw by these exceptions: SQLException, if was problem
     *                                 with reading from database or writing to database.
     *                                 AuthorizeException, if was problem with authorization to item
     *                                 fields. ContextException, if was problem with creating
     *                                 context of DSpace.
     */
    @POST
    @Path("/{item_id}/metadata")
    @ApiOperation(value = "Add metadata to an item by using the internal DSpace item identifier.",
            response = org.dspace.rest.common.Bitstream.class
    )
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response addItemMetadata(
            @ApiParam(value = "The identifier of the item.", required = true)
            @PathParam("item_id") Integer itemId,

            @ApiParam(value = "List of metadata objects", required = true)
                    List<MetadataEntry> metadata,

            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException {

        log.info("Adding metadata to item(id=" + itemId + ").");
        org.dspace.core.Context context = null;

        try {
            context = createContext();
            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.WRITE);

            writeStats(dspaceItem, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor, headers, request, context);

            for (MetadataEntry entry : metadata) {
                // TODO Test with Java split
                String data[] = mySplit(entry.getKey()); // Done by my split, because of java split was not function.
                if ((data.length >= 2) && (data.length <= 3)) {
                    dspaceItem.addMetadata(data[0], data[1], data[2], entry.getLanguage(), entry.getValue());
                }
            }
            dspaceItem.update();
            context.complete();

        } catch (SQLException e) {
            processException("Could not write metadata to item(id=" + itemId + "), SQLException. Message: " + e, context);
        } catch (AuthorizeException e) {
            processException("Could not write metadata to item(id=" + itemId + "), AuthorizeException. Message: " + e, context);
        } catch (ContextException e) {
            processException("Could not write metadata to item(id=" + itemId + "), ContextException. Message: " + e.getMessage(),
                    context);
        } finally {
            processFinally(context);
        }

        log.info("Metadata to item(id=" + itemId + ") were successfully added.");
        return Response.status(Status.OK).build();
    }

    /**
     * Create bitstream in item.
     *
     * @param itemId      Id of item in DSpace.
     * @param inputStream Data of bitstream in inputStream.
     * @param headers     If you want to access to item under logged user into context.
     *                    In headers must be set header "rest-dspace-token" with passed
     *                    token from login method.
     * @return Returns bitstream with status code OK(200). If id of item is
     * invalid , it returns status code NOT_FOUND(404). If user is not
     * allowed to write to item, UNAUTHORIZED(401).
     * @throws WebApplicationException It is thrown by these exceptions: SQLException, when was
     *                                 problem with reading/writing from/to database.
     *                                 AuthorizeException, when was problem with authorization to
     *                                 item and add bitstream to item. IOException, when was problem
     *                                 with creating file or reading from inpustream.
     *                                 ContextException. When was problem with creating context of
     *                                 DSpace.
     */
    // TODO Add option to add bitstream by URI.(for very big files)
    @POST
    @Path("/{item_id}/bitstreams")
    @ApiOperation(value = "Create a bitstream in an item by using the internal DSpace item identifier.",
            response = org.dspace.rest.common.Bitstream.class
    )
    public Bitstream addItemBitstream(
            @ApiParam(value = "The identifier of the item.", required = true)
            @PathParam("item_id") Integer itemId,

            @ApiParam(value = "InputStream object", required = true)
                    InputStream inputStream,

            @ApiParam(value = "The name of the bitstream.", required = true)
            @QueryParam("name") String name,

            @ApiParam(value = "The description of the bitstream.", required = false)
            @QueryParam("description") String description,

            @ApiParam(value = "The group id of the policy group.", required = false)
            @QueryParam("groupId") Integer groupId,

            @ApiParam(value = "The year of the policy start date.", required = false)
            @QueryParam("year") Integer year,

            @ApiParam(value = "The month of the policy start date.", required = false)
            @QueryParam("month") Integer month,

            @ApiParam(value = "The day of the policy start date.", required = false)
            @QueryParam("day") Integer day,

            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException {

        log.info("Adding bitstream to item(id=" + itemId + ").");
        org.dspace.core.Context context = null;
        Bitstream bitstream = null;

        try {
            context = createContext();
            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.WRITE);

            writeStats(dspaceItem, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor, headers, request, context);

            // Is better to add bitstream to ORIGINAL bundle or to item own?
            log.trace("Creating bitstream in item.");
            Bundle bundle = null;
            org.dspace.content.Bitstream dspaceBitstream = null;
            Bundle[] bundles = dspaceItem.getBundles("ORIGINAL");
            if (bundles != null && bundles.length != 0) {
                bundle = bundles[0]; // There should be only one bundle ORIGINAL.
            }
            if (bundle == null) {
                log.trace("Creating bundle in item.");
                dspaceBitstream = dspaceItem.createSingleBitstream(inputStream);
            } else {
                log.trace("Getting bundle from item.");
                dspaceBitstream = bundle.createBitstream(inputStream);
            }

            dspaceBitstream.setSource("DSpace Rest api");

            // Set bitstream name and description
            if (name != null) {
                if (BitstreamResource.getMimeType(name) == null) {
                    dspaceBitstream.setFormat(BitstreamFormat.findUnknown(context));
                } else {
                    dspaceBitstream.setFormat(BitstreamFormat.findByMIMEType(context, BitstreamResource.getMimeType(name)));
                }
                dspaceBitstream.setName(name);
            }
            if (description != null) {
                dspaceBitstream.setDescription(description);
            }

            dspaceBitstream.update();

            // Create policy for bitstream
            if (groupId != null) {
                bundles = dspaceBitstream.getBundles();
                for (Bundle dspaceBundle : bundles) {
                    List<org.dspace.authorize.ResourcePolicy> bitstreamsPolicies = dspaceBundle.getBitstreamPolicies();

                    // Remove default bitstream policies
                    List<org.dspace.authorize.ResourcePolicy> policiesToRemove = new ArrayList<org.dspace.authorize.ResourcePolicy>();
                    for (org.dspace.authorize.ResourcePolicy policy : bitstreamsPolicies) {
                        if (policy.getResourceID() == dspaceBitstream.getID()) {
                            policiesToRemove.add(policy);
                        }
                    }
                    for (org.dspace.authorize.ResourcePolicy policy : policiesToRemove) {
                        bitstreamsPolicies.remove(policy);
                    }

                    org.dspace.authorize.ResourcePolicy dspacePolicy = org.dspace.authorize.ResourcePolicy.create(context);
                    dspacePolicy.setAction(org.dspace.core.Constants.READ);
                    dspacePolicy.setGroup(Group.find(context, groupId));
                    dspacePolicy.setResourceID(dspaceBitstream.getID());
                    dspacePolicy.setResource(dspaceBitstream);
                    dspacePolicy.setResourceType(org.dspace.core.Constants.BITSTREAM);
                    if ((year != null) || (month != null) || (day != null)) {
                        Date date = new Date();
                        if (year != null) {
                            date.setYear(year - 1900);
                        }
                        if (month != null) {
                            date.setMonth(month - 1);
                        }
                        if (day != null) {
                            date.setDate(day);
                        }
                        date.setHours(0);
                        date.setMinutes(0);
                        date.setSeconds(0);
                        dspacePolicy.setStartDate(date);
                    }

                    dspacePolicy.update();
                    dspaceBitstream.updateLastModified();
                }
            }

            dspaceBitstream = org.dspace.content.Bitstream.find(context, dspaceBitstream.getID());
            bitstream = new Bitstream(dspaceBitstream, "");

            context.complete();

        } catch (SQLException e) {
            processException("Could not create bitstream in item(id=" + itemId + "), SQLException. Message: " + e, context);
        } catch (AuthorizeException e) {
            processException("Could not create bitstream in item(id=" + itemId + "), AuthorizeException. Message: " + e, context);
        } catch (IOException e) {
            processException("Could not create bitstream in item(id=" + itemId + "), IOException Message: " + e, context);
        } catch (ContextException e) {
            processException(
                    "Could not create bitstream in item(id=" + itemId + "), ContextException Message: " + e.getMessage(), context);
        } finally {
            processFinally(context);
        }

        log.info("Bitstream(id=" + bitstream.getId() + ") was successfully added into item(id=" + itemId + ").");
        return bitstream;
    }

    /**
     * Replace all metadata in item with new passed metadata.
     *
     * @param itemId   Id of item in DSpace.
     * @param metadata List of metadata fields, which will replace old metadata in
     *                 item.
     * @param headers  If you want to access to item under logged user into context.
     *                 In headers must be set header "rest-dspace-token" with passed
     *                 token from login method.
     * @return It returns status code: OK(200). NOT_FOUND(404) if item was not
     * found, UNAUTHORIZED(401) if user is not allowed to write to item.
     * @throws WebApplicationException It is thrown by: SQLException, when was problem with database
     *                                 reading or writting, AuthorizeException when was problem with
     *                                 authorization to item and metadata fields. And
     *                                 ContextException, when was problem with creating context of
     *                                 DSpace.
     */
    @PUT
    @Path("/{item_id}/metadata")
    @ApiOperation(value = "Replace the metadata of an item by using the internal DSpace item identifier.",
            response = Response.class
    )
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response updateItemMetadata(
            @ApiParam(value = "The identifier of the item.", required = true)
            @PathParam("item_id") Integer itemId,

            @ApiParam(value = "Array of metadata objects", required = true)
                    MetadataEntry[] metadata,

            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException {

        log.info("Updating metadata in item(id=" + itemId + ").");
        org.dspace.core.Context context = null;

        try {
            context = createContext();
            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.WRITE);

            writeStats(dspaceItem, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor, headers, request, context);

            log.trace("Deleting original metadata from item.");
            for (MetadataEntry entry : metadata) {
                String data[] = mySplit(entry.getKey());
                if ((data.length >= 2) && (data.length <= 3)) {
                    dspaceItem.clearMetadata(data[0], data[1], data[2], org.dspace.content.Item.ANY);
                }
            }

            log.trace("Adding new metadata to item.");
            for (MetadataEntry entry : metadata) {
                String data[] = mySplit(entry.getKey());
                if ((data.length >= 2) && (data.length <= 3)) {
                    dspaceItem.addMetadata(data[0], data[1], data[2], entry.getLanguage(), entry.getValue());
                }
            }

            dspaceItem.update();
            context.complete();

        } catch (SQLException e) {
            processException("Could not update metadata in item(id=" + itemId + "), SQLException. Message: " + e, context);
        } catch (AuthorizeException e) {
            processException("Could not update metadata in item(id=" + itemId + "), AuthorizeException. Message: " + e, context);
        } catch (ContextException e) {
            processException(
                    "Could not update metadata in item(id=" + itemId + "), ContextException. Message: " + e.getMessage(), context);
        } finally {
            processFinally(context);
        }

        log.info("Metadata of item(id=" + itemId + ") were successfully updated.");
        return Response.status(Status.OK).build();
    }

    /**
     * Delete item from DSpace. It delete bitstreams only from item bundle.
     *
     * @param itemId  Id of item which will be deleted.
     * @param headers If you want to access to item under logged user into context.
     *                In headers must be set header "rest-dspace-token" with passed
     *                token from login method.
     * @return It returns status code: OK(200). NOT_FOUND(404) if item was not
     * found, UNAUTHORIZED(401) if user is not allowed to delete item
     * metadata.
     * @throws WebApplicationException It can be thrown by: SQLException, when was problem with
     *                                 database reading. AuthorizeException, when was problem with
     *                                 authorization to item.(read and delete) IOException, when was
     *                                 problem with deleting bitstream file. ContextException, when
     *                                 was problem with creating context of DSpace.
     */
    @DELETE
    @Path("/{item_id}")
    @ApiOperation(value = "Delete and item by using the internal DSpace item identifier.",
            response = Response.class
    )
    public Response deleteItem(
            @ApiParam(value = "The identifier of the item.", required = true)
            @PathParam("item_id") Integer itemId,

            @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException {

        log.info("Deleting item(id=" + itemId + ").");
        org.dspace.core.Context context = null;

        try {
            context = createContext();
            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.DELETE);

            writeStats(dspaceItem, UsageEvent.Action.REMOVE, user_ip, user_agent, xforwardedfor, headers, request, context);

            log.trace("Deleting item.");
            org.dspace.content.Collection collection = org.dspace.content.Collection.find(context,
                    dspaceItem.getCollections()[0].getID());
            collection.removeItem(dspaceItem);
            context.complete();

        } catch (SQLException e) {
            processException("Could not delete item(id=" + itemId + "), SQLException. Message: " + e, context);
        } catch (AuthorizeException e) {
            processException("Could not delete item(id=" + itemId + "), AuthorizeException. Message: " + e, context);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            processException("Could not delete item(id=" + itemId + "), IOException. Message: " + e, context);
        } catch (ContextException e) {
            processException("Could not delete item(id=" + itemId + "), ContextException. Message: " + e.getMessage(), context);
        } finally {
            processFinally(context);
        }

        log.info("Item(id=" + itemId + ") was successfully deleted.");
        return Response.status(Status.OK).build();
    }

    /**
     * Delete all item metadata.
     *
     * @param itemId  Id of item in DSpace.
     * @param headers If you want to access to item under logged user into context.
     *                In headers must be set header "rest-dspace-token" with passed
     *                token from login method.
     * @return It returns status code: OK(200). NOT_FOUND(404) if item was not
     * found, UNAUTHORIZED(401) if user is not allowed to delete item
     * metadata.
     * @throws WebApplicationException It is thrown by three exceptions. SQLException, when was
     *                                 problem with reading item from database or editting metadata
     *                                 fields. AuthorizeException, when was problem with
     *                                 authorization to item. And ContextException, when was problem
     *                                 with creating context of DSpace.
     */
    @DELETE
    @Path("/{item_id}/metadata")
    @ApiOperation(value = "Delete all metadata of an item by using the internal DSpace item identifier.",
            response = Response.class
    )
    public Response deleteItemMetadata(
            @ApiParam(value = "The identifier of the item.", required = true)
            @PathParam("item_id") Integer itemId,

            @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException {

        log.info("Deleting metadata in item(id=" + itemId + ").");
        org.dspace.core.Context context = null;

        try {
            context = createContext();
            org.dspace.content.Item dspaceItem = findItem(context, itemId, org.dspace.core.Constants.WRITE);

            writeStats(dspaceItem, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor, headers, request, context);

            log.trace("Deleting metadata.");
            // TODO Rewrite without deprecated object. Leave there only generated metadata.
            Metadatum[] value = dspaceItem.getMetadata("dc", "date", "accessioned", org.dspace.content.Item.ANY);
            Metadatum[] value2 = dspaceItem.getMetadata("dc", "date", "available", org.dspace.content.Item.ANY);
            Metadatum[] value3 = dspaceItem.getMetadata("dc", "identifier", "uri", org.dspace.content.Item.ANY);
            Metadatum[] value4 = dspaceItem.getMetadata("dc", "description", "provenance", org.dspace.content.Item.ANY);

            dspaceItem.clearMetadata(org.dspace.content.Item.ANY, org.dspace.content.Item.ANY, org.dspace.content.Item.ANY,
                    org.dspace.content.Item.ANY);
            dspaceItem.update();

            // Add there generated metadata
            dspaceItem.addMetadata(value[0].schema, value[0].element, value[0].qualifier, null, value[0].value);
            dspaceItem.addMetadata(value2[0].schema, value2[0].element, value2[0].qualifier, null, value2[0].value);
            dspaceItem.addMetadata(value3[0].schema, value3[0].element, value3[0].qualifier, null, value3[0].value);
            dspaceItem.addMetadata(value4[0].schema, value4[0].element, value4[0].qualifier, null, value4[0].value);

            dspaceItem.update();
            context.complete();

        } catch (SQLException e) {
            processException("Could not delete item(id=" + itemId + "), SQLException. Message: " + e, context);
        } catch (AuthorizeException e) {
            processException("Could not delete item(id=" + itemId + "), AuthorizeExcpetion. Message: " + e, context);
        } catch (ContextException e) {
            processException("Could not delete item(id=" + itemId + "), ContextException. Message:" + e.getMessage(), context);
        } finally {
            processFinally(context);
        }

        log.info("Item(id=" + itemId + ") metadata were successfully deleted.");
        return Response.status(Status.OK).build();
    }

    /**
     * Delete bitstream from item bundle.
     *
     * @param itemId      Id of item in DSpace.
     * @param headers     If you want to access to item under logged user into context.
     *                    In headers must be set header "rest-dspace-token" with passed
     *                    token from login method.
     * @param bitstreamId Id of bitstream, which will be deleted from bundle.
     * @return Return status code OK(200) if is all ok. NOT_FOUND(404) if item
     * or bitstream was not found. UNAUTHORIZED(401) if user is not
     * allowed to delete bitstream.
     * @throws WebApplicationException It is thrown, when: Was problem with edditting database,
     *                                 SQLException. Or problem with authorization to item, bundle
     *                                 or bitstream, AuthorizeException. When was problem with
     *                                 deleting file IOException. Or problem with creating context
     *                                 of DSpace, ContextException.
     */
    @DELETE
    @Path("/{item_id}/bitstreams/{bitstream_id}")
    @ApiOperation(value = "Delete a bitstream of an item by using the internal DSpace item and bitstream identifiers.",
            response = Response.class
    )
    public Response deleteItemBitstream(
            @ApiParam(value = "The identifier of the item.", required = true)
            @PathParam("item_id") Integer itemId,

            @ApiParam(value = "The identifier of the bitstream.", required = true)
            @PathParam("bitstream_id") Integer bitstreamId,

            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException {

        log.info("Deleting bitstream in item(id=" + itemId + ").");
        org.dspace.core.Context context = null;

        try {
            context = createContext();
            org.dspace.content.Item item = findItem(context, itemId, org.dspace.core.Constants.WRITE);

            org.dspace.content.Bitstream bitstream = org.dspace.content.Bitstream.find(context, bitstreamId);
            if (bitstream == null) {
                context.abort();
                log.warn("Bitstream(id=" + bitstreamId + ") was not found.");
                return Response.status(Status.NOT_FOUND).build();
            } else if (!AuthorizeManager.authorizeActionBoolean(context, bitstream, org.dspace.core.Constants.DELETE)) {
                context.abort();
                log.error("User(" + getUser(headers).getEmail() + ") is not allowed to delete bitstream(id=" + bitstreamId + ").");
                return Response.status(Status.UNAUTHORIZED).build();
            }

            writeStats(item, UsageEvent.Action.UPDATE, user_ip, user_agent, xforwardedfor, headers, request, context);
            writeStats(bitstream, UsageEvent.Action.REMOVE, user_ip, user_agent, xforwardedfor, headers,
                    request, context);

            log.trace("Deleting bitstream...");
            for (Bundle bundle : item.getBundles()) {
                for (org.dspace.content.Bitstream bit : bundle.getBitstreams()) {
                    if (bit == bitstream) {
                        bundle.removeBitstream(bitstream);
                    }
                }
            }

            context.complete();

        } catch (SQLException e) {
            processException("Could not delete bitstream(id=" + bitstreamId + "), SQLException. Message: " + e, context);
        } catch (AuthorizeException e) {
            processException("Could not delete bitstream(id=" + bitstreamId + "), AuthorizeException. Message: " + e, context);
        } catch (IOException e) {
            processException("Could not delete bitstream(id=" + bitstreamId + "), IOException. Message: " + e, context);
        } catch (ContextException e) {
            processException("Could not delete bitstream(id=" + bitstreamId + "), ContextException. Message:" + e.getMessage(),
                    context);
        } finally {
            processFinally(context);
        }

        log.info("Bitstream(id=" + bitstreamId + ") from item(id=" + itemId + ") was successfuly deleted .");
        return Response.status(Status.OK).build();
    }

    /**
     * Find items by one metadada field.
     *
     * @param metadataEntry Metadata field to search by.
     * @param scheme        Scheme of metadata(key).
     * @param value         Value of metadata field.
     * @param headers       If you want to access the item as the user logged into context,
     *                      header "rest-dspace-token" must be set to token value retrieved
     *                      from the login method.
     * @return Return array of found items.
     * @throws WebApplicationException Can be thrown: SQLException - problem with
     *                                 database reading. AuthorizeException - problem with
     *                                 authorization to item. IOException - problem with
     *                                 reading from metadata field. ContextException -
     *                                 problem with creating DSpace context.
     */
    @POST
    @Path("/find-by-metadata-field")
    @ApiOperation(value = "Find items by a metadata field.",
            response = org.dspace.rest.common.Item[].class
    )
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Item[] findItemsByMetadataField(
            @ApiParam(value = "metadataEntry object", required = true)
                    MetadataEntry metadataEntry,

            @ApiParam(value = "Show additional data for the item.", required = false, allowMultiple = true, allowableValues = "all,metadata,parentCollection,parentCollectionList,parentCommunityList,bitstreams")
            @QueryParam("expand") String expand,

            @QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent,
            @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers, @Context HttpServletRequest request)
            throws WebApplicationException {

        log.info("Looking for item with metadata(key=" + metadataEntry.getKey() + ",value=" + metadataEntry.getValue()
                + ", language=" + metadataEntry.getLanguage() + ").");
        org.dspace.core.Context context = null;

        List<Item> items = new ArrayList<Item>();
        String[] metadata = mySplit(metadataEntry.getKey());

        try {
            context = createContext();

            // TODO Repair, it ends by error:
            // "java.sql.SQLSyntaxErrorException: ORA-00932: inconsistent datatypes: expected - got CLOB"
            /*
             * if (metadata.length == 3){
             *     itemIterator =  org.dspace.content.Item.findByMetadataField(context, metadata[0],
             *     metadata[1], metadata[2], value);
             * } else if (metadata.length == 2){
             *     itemIterator = org.dspace.content.Item.findByMetadataField(context, metadata[0],
             *     metadata[1], null, value);
             * } else {
             *     context.abort();
             *     log.error("Finding failed, bad metadata key.");
             *     throw new WebApplicationException(Response.Status.NOT_FOUND);
             * }
             *
             * if (itemIterator.hasNext()) {
             * item = new Item(itemIterator.next(), "", context);
             * }
             */

            // Must used own style.
            if ((metadata.length < 2) || (metadata.length > 3)) {
                context.abort();
                log.error("Finding failed, bad metadata key.");
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            List<Object> parameterList = new LinkedList<>();
            String sql = "SELECT RESOURCE_ID, TEXT_VALUE, TEXT_LANG, SHORT_ID, ELEMENT, QUALIFIER " +
                    "FROM METADATAVALUE " +
                    "JOIN METADATAFIELDREGISTRY ON METADATAVALUE.METADATA_FIELD_ID = METADATAFIELDREGISTRY.METADATA_FIELD_ID " +
                    "JOIN METADATASCHEMAREGISTRY ON METADATAFIELDREGISTRY.METADATA_SCHEMA_ID = METADATASCHEMAREGISTRY.METADATA_SCHEMA_ID " +
                    "WHERE " +
                    "SHORT_ID= ? AND " +
                    "ELEMENT= ? AND ";
            parameterList.add(metadata[0]);
            parameterList.add(metadata[1]);
            if (metadata.length > 3) {
                sql += "QUALIFIER= ? AND ";
                parameterList.add(metadata[2]);
            }
            if (org.dspace.storage.rdbms.DatabaseManager.isOracle()) {
                sql += "dbms_lob.compare(TEXT_VALUE, ? ) = 0 AND ";
                parameterList.add(metadataEntry.getValue());
            } else {
                sql += "TEXT_VALUE= ? AND ";
                parameterList.add(metadataEntry.getValue());
            }
            if (metadataEntry.getLanguage() != null) {
                sql += "TEXT_LANG= ? ";
                parameterList.add(metadataEntry.getLanguage());
            } else {
                sql += "TEXT_LANG is null";
            }

            Object[] parameters = parameterList.toArray();
            TableRowIterator iterator = org.dspace.storage.rdbms.DatabaseManager.query(context, sql, parameters);
            while (iterator.hasNext()) {
                TableRow row = iterator.next();
                org.dspace.content.Item dspaceItem = this.findItem(context, row.getIntColumn("RESOURCE_ID"),
                        org.dspace.core.Constants.READ);
                Item item = new Item(dspaceItem, expand, context);
                writeStats(dspaceItem, UsageEvent.Action.VIEW, user_ip, user_agent, xforwardedfor, headers,
                        request, context);
                items.add(item);
            }

            context.complete();

        } catch (SQLException e) {
            processException("Something went wrong while finding item. SQLException, Message: " + e, context);
        } catch (ContextException e) {
            processException("Context error:" + e.getMessage(), context);
        } finally {
            processFinally(context);
        }

        if (items.size() == 0) {
            log.info("Items not found.");
        } else {
            log.info("Items were found.");
        }

        return items.toArray(new Item[0]);
    }

    /**
     * Find item from DSpace database. It is encapsulation of method
     * org.dspace.content.Item.find with checking if item exist and if user
     * logged into context has permission to do passed action.
     *
     * @param context Context of actual logged user.
     * @param id      Id of item in DSpace.
     * @param action  Constant from org.dspace.core.Constants.
     * @return It returns DSpace item.
     * @throws WebApplicationException Is thrown when item with passed id is not exists and if user
     *                                 has no permission to do passed action.
     */
    private org.dspace.content.Item findItem(org.dspace.core.Context context, int id, int action) throws WebApplicationException {
        org.dspace.content.Item item = null;
        try {
            item = org.dspace.content.Item.find(context, id);

            if (item == null) {
                context.abort();
                log.warn("Item(id=" + id + ") was not found!");
                throw new WebApplicationException(Status.NOT_FOUND);
            } else if (!AuthorizeManager.authorizeActionBoolean(context, item, action)) {
                context.abort();
                if (context.getCurrentUser() != null) {
                    log.error("User(" + context.getCurrentUser().getEmail() + ") has not permission to "
                            + getActionString(action) + " item!");
                } else {
                    log.error("User(anonymous) has not permission to " + getActionString(action) + " item!");
                }
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

        } catch (SQLException e) {
            processException("Something get wrong while finding item(id=" + id + "). SQLException, Message: " + e, context);
        }
        return item;
    }
}
