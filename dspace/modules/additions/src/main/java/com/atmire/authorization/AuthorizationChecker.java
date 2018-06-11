package com.atmire.authorization;

import com.atmire.authorization.checks.AuthorizationCheck;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Required;

import java.util.List;

/**
 * Created by jonas - jonas@atmire.com on 13/04/16.
 */
public class AuthorizationChecker {

    protected List<AuthorizationCheck> authorizationChecks;

    @Required
    public void setAuthorizationChecks(List<AuthorizationCheck> authorizationChecks) {
        this.authorizationChecks = authorizationChecks;
    }

    public boolean checkAuthorization(Context context, DSpaceObject dso) {

        if (context.getCurrentUser() == null || dso == null) {
            return false;
        }
        for (AuthorizationCheck check : authorizationChecks) {
            if (check.checkAuthorization(context, dso)) {
                return true;
            }
        }
        return false;
    }

}
