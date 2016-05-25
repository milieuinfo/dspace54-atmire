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
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Constants;
import org.dspace.handle.HandleManager;
import org.dspace.rest.common.Collection;
import org.dspace.rest.common.Community;
import org.dspace.rest.common.DSpaceObject;
import org.dspace.rest.common.Item;
import org.dspace.rest.exceptions.ContextException;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 10/7/13
 * Time: 1:54 PM
 * To change this template use File | Settings | File Templates.
 */
@Path("/handle")
@Api(value = "/handle", description = "Retrieve a handle", position = 4)
public class HandleResource extends Resource
{
    private static Logger log = Logger.getLogger(HandleResource.class);
    private static org.dspace.core.Context context;

    @GET
    @Path("/{prefix}/{suffix}")
    @ApiOperation(value = "Retrieve a single object by using object pre- and suffix.",
            response = org.dspace.rest.common.DSpaceObject.class
    )
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public DSpaceObject getObject(
            @ApiParam(value = "The object prefix.", required = false)
            @PathParam("prefix") String prefix,

            @ApiParam(value = "The object suffix.", required = false)
            @PathParam("suffix") String suffix,

            @ApiParam(value = "Show additional data for the object.", required = false, allowMultiple = true)
            @QueryParam("expand") String expand,

            @Context HttpHeaders headers) throws WebApplicationException
    {
        org.dspace.core.Context context = null;
        DSpaceObject result = null;

        try
        {
            context = createContext(getUser(headers));

            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);
            if (dso == null)
            {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            log.info("DSO Lookup by handle: [" + prefix + "] / [" + suffix + "] got result of: " + dso.getTypeText() + "_" + dso.getID());

            if(AuthorizeManager.authorizeActionBoolean(context, dso, org.dspace.core.Constants.READ)) {
                switch (dso.getType())
                {
                    case Constants.COMMUNITY:
                        result = new Community((org.dspace.content.Community) dso, expand, context);
                        break;
                    case Constants.COLLECTION:
                        result =  new Collection((org.dspace.content.Collection) dso, expand, context, null, null);
                        break;
                    case Constants.ITEM:
                        result =  new Item((org.dspace.content.Item) dso, expand, context);
                        break;
                    default:
                        result = new DSpaceObject(dso);
                }
            } else
            {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

            context.complete();

        } catch (SQLException e)
        {
            processException("Could not read handle(" + prefix + "/" + suffix + "), SQLException. Message: " + e.getMessage(), context);
        } catch (ContextException e)
        {
            processException("Could not read handle(" + prefix + "/" + suffix + "), ContextException. Message: " + e.getMessage(), context);
        }
        catch (WebApplicationException e){
            log.error("Exception in HandleResource ",e);
            throw e;
        }
        catch (Throwable e){
            log.error("Exception in HandleResource ",e);
            throw e;
        }

        finally
        {
            processFinally(context);
        }

        return result;
    }
}