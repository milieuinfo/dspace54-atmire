package com.atmire.authorization.checks;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * Created by jonas - jonas@atmire.com on 13/04/16.
 */
public interface AuthorizationCheck {


    public boolean checkAuthorization(Context context, DSpaceObject dso);
}
