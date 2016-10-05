package org.dspace.app.itemexport;

import java.util.Date;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 31 Aug 2016
 */
public abstract class DateFile {

    public static final Date NO_DATE = null;

    public abstract Date getLastDate();

    public abstract void writeDate();
}
