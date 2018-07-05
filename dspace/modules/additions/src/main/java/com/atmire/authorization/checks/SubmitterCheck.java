package com.atmire.authorization.checks;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import java.sql.SQLException;

/**
 * Created by jonas - jonas@atmire.com on 13/04/16.
 */
public class SubmitterCheck implements AuthorizationCheck {
    /* Log4j logger*/
    private static final Logger log = Logger.getLogger(SubmitterCheck.class);

    @Override
    public boolean checkAuthorization(Context context, DSpaceObject dso) {
        if (dso instanceof Item) {
            try {
                EPerson submitter = ((Item) dso).getSubmitter();
                if (getId(context.getCurrentUser()) == getId(submitter)) {
                    return true;
                }
            } catch (SQLException e) {
                log.error("Error while checking if the current user is the submitter of item with handle: " + dso.getHandle(), e);
            }
        }
        return false;
    }

    private int getId(EPerson user) {
        return user != null ? user.getID() : -1;
    }
}
