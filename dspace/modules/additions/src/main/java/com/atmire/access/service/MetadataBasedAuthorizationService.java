package com.atmire.access.service;

import org.dspace.content.*;
import org.dspace.core.*;
import org.dspace.eperson.*;

/**
 * @author philip at atmire.com
 */
public interface MetadataBasedAuthorizationService {

    boolean isAuthorized(Context context, EPerson eperson, Group group, Item item);
}
