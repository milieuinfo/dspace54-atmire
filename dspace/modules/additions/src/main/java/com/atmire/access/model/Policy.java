package com.atmire.access.model;

import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.eperson.*;

import java.util.List;

/**
 * @author philip at atmire.com
 */
public interface Policy {

    boolean isAuthorized(EPerson ePerson, Item item);

    String getSolrIndexField();

    List<String> getSolrIndexValues(Context context, DSpaceObject dSpaceObject);

    String getSolrQueryCriteria(EPerson ePerson);
}
