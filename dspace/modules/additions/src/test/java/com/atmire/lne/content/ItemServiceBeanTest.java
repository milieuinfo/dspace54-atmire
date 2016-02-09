package com.atmire.lne.content;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ItemServiceBeanTest {

    @InjectMocks
    private ItemServiceBean serviceBean = new ItemServiceBean();

    @Mock
    private SearchService searchService;

    @Mock
    private Context context;

    @Mock
    private Item item;

    @Mock
    private Collection collection;


    @Test
    public void testFindItemsByExternalHandle() throws Exception {
        DiscoverResult discoverResult = new DiscoverResult();
        discoverResult.addDSpaceObject(item);
        discoverResult.addDSpaceObject(collection);

        when(searchService.search(eq(context),
                Matchers.argThat(new ArgumentMatcher<DiscoverQuery>() {
                                     public boolean matches(final Object argument) {
                                         DiscoverQuery query = (DiscoverQuery) argument;
                                         return StringUtils.equals(ItemServiceBean.EXTERNAL_HANDLE_DISCOVERY_FIELD
                                                 + "_keyword : " + "1234", query.getQuery())
                                                 && Constants.ITEM == query.getDSpaceObjectFilter();
                                     }
                                 }
                ))).thenReturn(discoverResult);

        List<Item> results = serviceBean.findItemsByExternalHandle(context, "1234");

        assertEquals(Arrays.asList(item), results);
    }

    @Test
    public void testFindItemsByExternalHandleEmpty() throws Exception {
        DiscoverResult discoverResult = new DiscoverResult();

        when(searchService.search(eq(context),
                Matchers.argThat(new ArgumentMatcher<DiscoverQuery>() {
                                     public boolean matches(final Object argument) {
                                         DiscoverQuery query = (DiscoverQuery) argument;
                                         return StringUtils.equals(ItemServiceBean.EXTERNAL_HANDLE_DISCOVERY_FIELD
                                                 + "_keyword : " + "1234", query.getQuery())
                                                 && Constants.ITEM == query.getDSpaceObjectFilter();
                                     }
                                 }
                ))).thenReturn(discoverResult);

        List<Item> results = serviceBean.findItemsByExternalHandle(context, "1234");

        assertTrue(results.isEmpty());
    }

}