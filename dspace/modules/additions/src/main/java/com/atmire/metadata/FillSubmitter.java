package com.atmire.metadata;

import org.apache.commons.lang.UnhandledException;
import org.dspace.content.DCPersonName;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.eperson.EPerson;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 07 Jun 2018
 */
public class FillSubmitter extends AbstractFillValue {

    private List<String> getSubmitter(EditParameters parameters) {
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
        return emptyIfNull(value);
    }

    @Override
    public List<Metadatum> getValues(EditParameters parameters) {
        return convert(getSubmitter(parameters));
    }

}
