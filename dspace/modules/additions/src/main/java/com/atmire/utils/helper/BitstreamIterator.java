package com.atmire.utils.helper;

import org.dspace.content.Bitstream;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 21 Jan 2016
 */
public class BitstreamIterator implements Iterator<Bitstream> {

    protected Context context;
    protected Iterator<Integer> idIterator;

    public BitstreamIterator(Context context, List<Integer> bitstreamIds) {
        this.context = context;
        idIterator = bitstreamIds.iterator();
    }

    @Override
    public boolean hasNext() {
        return idIterator.hasNext();
    }

    @Override
    public Bitstream next() {
        Integer nextID = idIterator.next();
        Bitstream bitstream;
        try {
            bitstream = Bitstream.find(context, nextID);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return bitstream;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
