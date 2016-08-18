package com.atmire.access.service;

import com.atmire.access.model.Policy;
import org.dspace.content.*;
import org.dspace.core.*;
import org.dspace.eperson.*;

import java.util.List;

/**
 * @author philip at atmire.com
 */
public interface MetadataBasedAuthorizationService {

    boolean isAuthorized(Context context, EPerson eperson, Group group, DSpaceObject item);

    List<Policy> retrievePoliciesForGroup(Group group);
}