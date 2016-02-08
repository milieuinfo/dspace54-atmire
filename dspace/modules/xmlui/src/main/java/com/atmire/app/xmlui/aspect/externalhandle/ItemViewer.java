package com.atmire.app.xmlui.aspect.externalhandle;

import com.atmire.lne.content.ItemService;
import com.atmire.lne.content.ItemServiceBean;
import com.atmire.lne.exception.MetaDataFieldNotSetException;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.discovery.SearchServiceException;
import org.springframework.web.util.HtmlUtils;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

//TODO TOM UNIT TEST
public class ItemViewer extends AbstractDSpaceTransformer {

    private static final Logger log = Logger.getLogger(ItemViewer.class);

    private static final Message T_error_page = message("xmlui.externalhandle.error_page");
    private static final Message T_metadata_field_problem = message("xmlui.externalhandle.metadata_field_problem");
    private static final Message T_discovery_query_problem = message("xmlui.externalhandle.discovery_query_problem");

    private static final Message T_not_found_page = message("xmlui.externalhandle.not_found_page");
    private static final Message T_requested_item_not_found = message("xmlui.externalhandle.item_not_found");

    private static final Message T_too_many_items = message("xmlui.externalhandle.too_many_items_page");
    private static final Message T_external_handle_not_unique = message("xmlui.externalhandle.external_handle_not_unique");

    private ItemService itemService;

    public ItemViewer() {
        //TODO TOM can this be autowired?
        itemService = new ItemServiceBean();
    }

    public void addBody(final Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException, ProcessingException {

        HttpServletResponse response = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);

        try {
            String externalHandle = getExternalHandle();

            List<Item> result = itemService.findItemsByExternalHandle(context, externalHandle);

            renderCorrectPage(body, response, result);

        } catch (MetaDataFieldNotSetException e) {
            renderErrorPage(body, response, T_metadata_field_problem, e);
        } catch (SearchServiceException e) {
            renderErrorPage(body, response, T_discovery_query_problem, e);
        }
    }

    private void renderCorrectPage(final Body body, final HttpServletResponse response, final List<Item> result) throws IOException, WingException {
        if (result.size() <= 0) {
            renderNotFoundPage(body, response);
        } else if(result.size() > 1) {
            renderTooManyResultsPage(body, response);
        } else {
            String handle = result.get(0).getHandle();
            renderItemPage(response, handle);
        }
    }

    private void renderItemPage(final HttpServletResponse response, final String handle) throws IOException {
        response.sendRedirect(buildRedirectUrl(handle));
    }

    private void renderTooManyResultsPage(final Body body, final HttpServletResponse response) throws WingException {
        Division errorMessage = body.addDivision("too-many-items-for-external-handle");
        errorMessage.setHead(T_too_many_items);
        errorMessage.addPara(T_external_handle_not_unique);

        response.setStatus(HttpServletResponse.SC_CONFLICT);
    }

    private void renderNotFoundPage(final Body body, final HttpServletResponse response) throws WingException {
        Division errorMessage = body.addDivision("external-handle-not-found");
        errorMessage.setHead(T_not_found_page);
        errorMessage.addPara(T_requested_item_not_found);

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    private void renderErrorPage(final Body body, final HttpServletResponse response, final Message messageKey, final Exception e) throws WingException {
        log.error(messageKey.getKey(), e);

        Division errorMessage = body.addDivision("error-message");
        errorMessage.setHead(T_error_page);
        errorMessage.addPara(messageKey);

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private String getExternalHandle() {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String rawValue = StringUtils.substringAfter(request.getSitemapURI(), "external-handle/");
        return HtmlUtils.htmlUnescape(rawValue);
    }

    private String buildRedirectUrl(final String dsItemHandle) {
        return contextPath + "/handle/" + dsItemHandle;
    }
}
