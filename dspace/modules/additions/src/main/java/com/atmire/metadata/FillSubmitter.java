package com.atmire.metadata;

import org.apache.commons.lang.UnhandledException;
import org.dspace.content.DCPersonName;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;

import java.sql.SQLException;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 07 Jun 2018
 */
public class FillSubmitter extends FillValue
{
    @Override
    public String getValue(FillValueDependencies parameters) {
        String value = null;
        if (parameters.getObject() instanceof Item) {
            Item item = (Item) parameters.getObject();
            try {
                EPerson submitter = item.getSubmitter();
                value = new DCPersonName(submitter.getLastName(), submitter.getFirstName())
                        .toString();
            } catch (SQLException e) {
                throw new UnhandledException(e);
            }
        }
        return value;
    }
}
