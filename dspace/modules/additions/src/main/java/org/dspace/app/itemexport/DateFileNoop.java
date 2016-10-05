package org.dspace.app.itemexport;

import java.util.Date;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 01 Sep 2016
 */
public class DateFileNoop extends DateFile {

    @Override
    public Date getLastDate() {
        return DateFile.NO_DATE;
    }

    @Override
    public void writeDate() {
    }
}
