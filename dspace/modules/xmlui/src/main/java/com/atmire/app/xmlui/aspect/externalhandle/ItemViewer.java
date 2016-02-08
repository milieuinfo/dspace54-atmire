package com.atmire.app.xmlui.aspect.externalhandle;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Constants;
import org.dspace.discovery.*;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class ItemViewer extends AbstractDSpaceTransformer {

    private static final String METADATA_FIELD_PARAMETER = "metadata-field";
    private static final String HANDLE_SEARCH_FIELD = "handle";

    private static final Logger log = Logger.getLogger(ItemViewer.class);

    private SearchService searchService = null;


    public void addBody(final Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException, ProcessingException {

        HttpServletResponse response = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);

        try {
            String metadataField = getMetadataField();
            String metadataValue = getMetaDataValue();

            DiscoverResult result = getSearchService().search(context, buildDiscoveryQuery(metadataField, metadataValue));

            renderCorrectPage(response, result);

        } catch (ParameterException e) {
            String description = "There was a problem reading the external handle metadata field name from the sitemap configuration: "
                    + e.getMessage();
            renderError(description, e);
        } catch (SearchServiceException e) {
            String description = "There was a problem executing the discovery query: "
                    + e.getMessage();
            renderError(description, e);
        }
    }

    private void renderError(final String description, final Exception e) {
        log.error(description, e);
        //TODO
    }

    private void renderCorrectPage(final HttpServletResponse response, final DiscoverResult result) throws IOException {
        if (result.getTotalSearchResults() <= 0) {
            renderNotFoundPage(response);
        } else if(result.getTotalSearchResults() > 1) {
            renderTooManyResultsPage(response);
        } else {
            String handle = result.getDspaceObjects().get(0).getHandle();
            renderItemPage(response, handle);
        }
    }

    private void renderItemPage(final HttpServletResponse response, final String handle) throws IOException {
        response.sendRedirect(buildRedirectUrl(handle));
    }

    private void renderTooManyResultsPage(final HttpServletResponse response) {
        //TODO add error message to body
        response.setStatus(HttpServletResponse.SC_CONFLICT);
    }

    private void renderNotFoundPage(final HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    private DiscoverQuery buildDiscoveryQuery(final String metadataField, final String metadataValue) {
        DiscoverQuery query = new DiscoverQuery();
        query.setDSpaceObjectFilter(Constants.ITEM);
        query.setQuery(metadataField + " : " + metadataValue);
        query.addSearchField(HANDLE_SEARCH_FIELD);

        return query;
    }

    public SearchService getSearchService() {
        if(searchService == null) {
            searchService = SearchUtils.getSearchService();
        }
        return searchService;
    }

    private String getMetadataField() throws ParameterException {
        return parameters.getParameter(METADATA_FIELD_PARAMETER);
    }

    protected String getMetaDataValue() {
        Request request = ObjectModelHelper.getRequest(objectModel);
        return StringUtils.substringAfter(request.getSitemapURI(), "external-handle/");
    }

    private String buildRedirectUrl(final String dsItemHandle) {
        return contextPath + "/handle/" + dsItemHandle;
    }
}
