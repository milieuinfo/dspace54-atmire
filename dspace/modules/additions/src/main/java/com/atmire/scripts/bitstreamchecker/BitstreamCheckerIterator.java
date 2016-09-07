package com.atmire.scripts.bitstreamchecker;

import com.atmire.utils.helper.BitstreamIterator;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamQuerier;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 05 Jul 2016
 */
public class BitstreamCheckerIterator implements java.util.Iterator<org.dspace.content.Bitstream> {

    private int queryLimit = 1000;
    private BitstreamIterator innerIterator;
    private Context context;


    public BitstreamCheckerIterator(Context context, int queryLimit) {
        this.context = context;
        this.queryLimit = queryLimit;
        nextIterator();
    }

    private void nextIterator() {
        List<Integer> ids = BitstreamQuerier.getInstance().findByVirusScanDate(context, queryLimit);
        this.innerIterator = new BitstreamIterator(context, ids);
    }

    @Override
    public boolean hasNext() {
        return innerIterator.hasNext();
    }

    @Override
    public Bitstream next() {
        Bitstream next = innerIterator.next();
        if (!innerIterator.hasNext()) {
            try {
                context.commit();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            nextIterator();
        }
        return next;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
