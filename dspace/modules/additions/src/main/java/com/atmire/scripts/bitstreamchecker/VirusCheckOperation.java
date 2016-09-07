package com.atmire.scripts.bitstreamchecker;

import com.atmire.scripts.PrintConsumer;
import com.atmire.utils.Consumer;
import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.ctask.general.ClamScan;

import java.io.IOException;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 04 Jul 2016
 */
public class VirusCheckOperation extends BitstreamCheckOperation {

    private static Logger log = Logger.getLogger(VirusCheckOperation.class);

    public VirusCheckOperation(String name) {
        super(name);
    }

    @Override
    public void check(Context context, Bitstream bitstream, final PrintConsumer printer) {
        try {
            new ClamScan(context, false) {
                @Override
                protected void report(String message) {
                    // only infected files cause messages here
                    printer.info(message);
                }
            }.perform(bitstream);
        } catch (IOException e) {
            log.error("", e);
        }
    }
}
