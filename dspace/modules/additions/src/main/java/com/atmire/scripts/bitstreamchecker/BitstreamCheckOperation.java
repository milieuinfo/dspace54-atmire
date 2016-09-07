package com.atmire.scripts.bitstreamchecker;

import com.atmire.scripts.PrintConsumer;
import com.atmire.utils.Consumer;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 04 Jul 2016
 */
public abstract class BitstreamCheckOperation {

    private String name;

    public BitstreamCheckOperation(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    abstract void check(Context context, Bitstream bitstream, PrintConsumer printer);
}
