package com.atmire.access.model;

import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.eperson.*;

/**
 * @author philip at atmire.com
 */
public interface Policy {

    boolean isAuthorized(EPerson ePerson, Item item);

    String getSolrIndexField();

    String getSolrIndexValue(Context context, DSpaceObject dSpaceObject);
}
