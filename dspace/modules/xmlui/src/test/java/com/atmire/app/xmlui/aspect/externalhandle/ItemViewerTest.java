package com.atmire.app.xmlui.aspect.externalhandle;

import com.atmire.lne.content.ItemService;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.util.HashMap;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ItemViewerTest {

    @InjectMocks
    private ItemViewer itemViewer = new ItemViewer() {
        public void setup(final SourceResolver resolver, final Map objectModel, final String src, final Parameters parameters) throws ProcessingException, SAXException, IOException {
            this.objectModel = objectModel;
            this.parameters = parameters;
            try {
                this.context = ContextUtil.obtainContext(objectModel);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            this.eperson = context.getCurrentUser();
            Request request = ObjectModelHelper.getRequest(objectModel);
            this.contextPath = "test.com";
        }
    };

    @Mock
    private ItemService itemService;

    @Mock
    private SourceResolver resolver;

    private Map objectModel;

    private Parameters parameters = new Parameters();

    @Mock
    private HttpServletResponse response;

    @Mock
    private Request request;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private Body body;

    @Mock
    private Item item;

    @Mock
    private Bundle bundle;

    @Mock
    private Bitstream bitstream;

    @Mock
    private Context context;


    @Before
    public void setUp() throws SAXException, SQLException, IOException, ProcessingException {
        objectModel = new HashMap();

        objectModel.put(HttpEnvironment.HTTP_RESPONSE_OBJECT, response);
        objectModel.put(ObjectModelHelper.REQUEST_OBJECT, request);

        when(request.getAttribute(ContextUtil.DSPACE_CONTEXT)).thenReturn(context);
        when(item.getHandle()).thenReturn("123456/789");

        parameters.setParameter("handle","1337");
        parameters.setParameter("action","default");


        when(item.getBundles()).thenReturn(new Bundle[]{bundle});
        when(bitstream.getName()).thenReturn("abc.jpg");
        when(bundle.getBitstreams()).thenReturn(new Bitstream[]{bitstream});

        itemViewer.setup(resolver, objectModel, "", parameters);
    }

    @Test
    public void testItemFound() throws Exception {
        when(itemService.findItemsByExternalHandle(context, "1337")).thenReturn(Arrays.asList(item));

        itemViewer.addBody(body);

        verify(response).sendRedirect("test.com/handle/123456/789");
    }

    @Test
    public void testItemNotFound() throws Exception {
        when(itemService.findItemsByExternalHandle(context, "1337")).thenReturn(new LinkedList<Item>());

        itemViewer.addBody(body);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testItemFoundTooMany() throws Exception {
        when(itemService.findItemsByExternalHandle(context, "1337")).thenReturn(Arrays.asList(item, item));

        itemViewer.addBody(body);

        verify(response).setStatus(HttpServletResponse.SC_CONFLICT);
    }


    @Test
    public void testBitstreamFound() throws Exception {
        parameters.setParameter("action","bitstream");


        when(itemService.findItemsByExternalHandle(context, "1337")).thenReturn(Arrays.asList(item));



        itemViewer.addBody(body);

        verify(response).sendRedirect("test.com/bitstream/handle/123456/789/abc.jpg");
    }

    @Test
    public void testBitstreamFoundManyBundles() throws Exception {
        parameters.setParameter("action","bitstream");

        when(item.getBundles()).thenReturn(new Bundle[]{bundle,bundle});
        when(itemService.findItemsByExternalHandle(context, "1337")).thenReturn(Arrays.asList(item));

        itemViewer.addBody(body);

        verify(response).setStatus(HttpServletResponse.SC_CONFLICT);
    }

    @Test
    public void testBitstreamFoundManyBitstreams() throws Exception {
        parameters.setParameter("action","bitstream");

        when(itemService.findItemsByExternalHandle(context, "1337")).thenReturn(Arrays.asList(item));

        when(bundle.getBitstreams()).thenReturn(new Bitstream[]{bitstream,bitstream});


        itemViewer.addBody(body);

        verify(response).setStatus(HttpServletResponse.SC_CONFLICT);
    }




    @Test
    public void testBitstreamNotFound() throws Exception {
        parameters.setParameter("action","bitstream");

        when(itemService.findItemsByExternalHandle(context, "1337")).thenReturn(Arrays.asList(item));
        when(item.getBundles()).thenReturn(new Bundle[]{});


        itemViewer.addBody(body);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testBitstreamFoundTooMany() throws Exception {
        parameters.setParameter("action","bitstream");

        when(itemService.findItemsByExternalHandle(context, "1337")).thenReturn(Arrays.asList(item, item));

        itemViewer.addBody(body);

        verify(response).setStatus(HttpServletResponse.SC_CONFLICT);
    }




    @Test
    public void testInternalError() throws Exception {
        when(itemService.findItemsByExternalHandle(context, "1337")).thenThrow(SearchServiceException.class);

        itemViewer.addBody(body);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }


}