package com.atmire.utils;

import java.sql.*;
import java.util.Date;

import org.apache.log4j.*;
import org.dspace.authorize.*;
import org.dspace.content.*;
import org.dspace.core.*;

/**
 * Created by Roeland Dillen (roeland at atmire dot com)
 * Date: 16/04/13
 * Time: 14:22
 */
public class EmbargoUtils {
    public static final int ANONYMOUS_GROUP_ID = 0;

    private static Logger log = Logger.getLogger(EmbargoUtils.class);


    public static Date getEmbargo(Bitstream bitstream, Context context) {
        java.util.List<ResourcePolicy> policiesByDSOAndType = null;
        try {
            policiesByDSOAndType = AuthorizeManager.getPoliciesActionFilter(
                    context,
                    bitstream,
                    Constants.READ
            );
        } catch (SQLException e) {
            return null;
        }

        for (ResourcePolicy pol : policiesByDSOAndType) {
            //This will be the start date of the Anonymous policy: Anonymous will get read access on
            Date date = pol.getStartDate();
            if (date != null)
                return date;

        }
        return null;
    }

    public static Date getLastEmbargo(Item item, Context context) {
        Date lastEmbargo = null;
        try {
            Bitstream[] bitstreams = item.getNonInternalBitstreams();

            for (Bitstream bitstream : bitstreams) {
                java.util.List<ResourcePolicy> policiesByDSOAndType
                        = AuthorizeManager.getPoliciesActionFilter(
                        context,
                        bitstream,
                        Constants.READ
                );

                for (ResourcePolicy pol : policiesByDSOAndType) {
                    if (pol.getGroupID() == ANONYMOUS_GROUP_ID) {
                        //This will be the start date of the Anonymous policy: Anonymous will get read access on
                        Date date = pol.getStartDate();
                        if (date != null)
                            if (lastEmbargo == null) {
                                lastEmbargo = date;
                            } else if (date.after(lastEmbargo)) {
                                lastEmbargo = date;
                            }
                    }
                }
            }

        } catch (Exception e) {
            log.error("error in in getting embargo action", e);

        }
        return lastEmbargo;
    }
}
