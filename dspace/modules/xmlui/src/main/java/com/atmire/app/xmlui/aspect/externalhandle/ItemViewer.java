package com.atmire.app.xmlui.aspect.externalhandle;

import com.atmire.lne.content.ItemService;
import com.atmire.lne.content.ItemServiceBean;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.discovery.SearchServiceException;
import org.xml.sax.SAXException;

public class ItemViewer extends AbstractDSpaceTransformer {

    private static final Logger log = Logger.getLogger(ItemViewer.class);

    private static final Message T_error_page = message("xmlui.externalhandle.error_page");
    private static final Message T_discovery_query_problem = message("xmlui.externalhandle.discovery_query_problem");

    private static final Message T_not_found_page = message("xmlui.externalhandle.not_found_page");
    private static final Message T_requested_item_not_found = message("xmlui.externalhandle.item_not_found");

    private static final Message T_too_many_items = message("xmlui.externalhandle.too_many_items_page");
    private static final Message T_external_handle_not_unique = message("xmlui.externalhandle.external_handle_not_unique");
    private static final Message T_related_items = message("xmlui.externalhandle.related_items");

    private static final int NOT_FOUND = 0;
    private static final int FOUND = 1;
    private static final int TOO_MANY = 2;

    private static final String PARAMETER_HANDLE = "handle";
    private static final String PARAMETER_ACTION = "action";
    private static final String ACTION_BITSTREAM = "bitstream";

    private ItemService itemService;

    public ItemViewer() {
        itemService = new ItemServiceBean();
    }

    public void addBody(final Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException, ProcessingException {

      HttpServletResponse response = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);

      try {

            String externalHandle = parameters.getParameter(PARAMETER_HANDLE);
            boolean downloadBitstream = parameters.getParameter(PARAMETER_ACTION).equals(ACTION_BITSTREAM);

            List<Item> result = itemService.findItemsByExternalHandle(context, externalHandle);

            renderCorrectPage(body, response, result,downloadBitstream);

        } catch (SearchServiceException| ParameterException e) {
            renderErrorPage(body, response, T_discovery_query_problem, e);
        }
    }

    private void renderCorrectPage(final Body body, final HttpServletResponse response, final List<Item> result, final boolean downloadBitstream) throws IOException, SQLException, WingException {
        if(downloadBitstream){
          handleBitstream(body,response,result);
        } else {
          handleItem(body,response,result);
        }

    }

    private void handleBitstream(final Body body, final HttpServletResponse response, final List<Item> result) throws SQLException, WingException, IOException {

      List<Bitstream> originalBitstreams = getOriginalBitstreams(result);
      switch (determinePageToRender(originalBitstreams.size())){
        case NOT_FOUND:
          renderNotFoundPage(body,response);
          break;
        case  TOO_MANY:
          renderTooManyResultsPage(body,response,result);
          break;
        case FOUND:
        default:
          redirectToBistream(response,getItemHandle(result),originalBitstreams.get(0));
          break;
      }
    }

    private List<Bitstream> getOriginalBitstreams(final List<Item> result) throws SQLException{
      List<Bitstream> toReturn = new ArrayList<>();
      for (Item item : result){
        for (Bundle bundle : item.getBundles()){
          if (bundle.getName().equalsIgnoreCase("original")){
            for(Bitstream bitstream: bundle.getBitstreams()) {
              toReturn.add(bitstream);
            }
          }
        }
      }
      return toReturn;
    }

    private void handleItem(final Body body, final HttpServletResponse response, final List<Item> result) throws SQLException, WingException, IOException {
      switch (itemPageToRender(result)){
        case NOT_FOUND:
          renderNotFoundPage(body,response);
          break;
        case  TOO_MANY:
          renderTooManyResultsPage(body,response,result);
          break;
        case FOUND:
        default:
          redirectToItemPage(response,getItemHandle(result));
          break;
      }
    }

    private String getItemHandle(List<Item> items) {
      return items.get(0).getHandle();
    }

    private int itemPageToRender(final List<Item> items){
      return determinePageToRender(items.size());
    }


    private int determinePageToRender(final int nbItems){
      switch (nbItems){
        case 0:
          return NOT_FOUND;
        case 1:
          return FOUND;
        default:
          return TOO_MANY;
      }
    }

    private void redirectToItemPage(final HttpServletResponse response, final String handle) throws IOException {
        response.sendRedirect(buildItemViewPageUrl(handle));
    }

    private void redirectToBistream(final HttpServletResponse response, final String handle, final Bitstream bitstream) throws IOException {
        response.sendRedirect(buildBitstreamPageUrl(handle,bitstream));
    }

    private void renderTooManyResultsPage(final Body body, final HttpServletResponse response, final List<Item> result) throws WingException {
        Division errorMessage = body.addDivision("too-many-items-for-external-handle");
        errorMessage.setHead(T_too_many_items);
        errorMessage.addPara(T_external_handle_not_unique);

        org.dspace.app.xmlui.wing.element.List itemList = errorMessage.addList("related-items");
        itemList.setHead(T_related_items);

        for (Item item : result) {
            itemList.addItemXref(buildItemViewPageUrl(item.getHandle()), item.getHandle());
        }

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

    private String buildItemViewPageUrl(final String dsItemHandle) {
        return contextPath + "/handle/" + dsItemHandle;
    }

    private String buildBitstreamPageUrl(final String dsItemHandle,Bitstream bitstream) {
        return  contextPath + "/bitstream/handle/" + dsItemHandle +"/"+bitstream.getName();
    }
}
