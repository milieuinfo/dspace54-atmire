package com.atmire.scripts;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.ArrayUtils;
import org.dspace.content.*;
import org.dspace.core.Utils;
import org.dspace.handle.HandleManager;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 21 May 2014
 */
public abstract class IteratorScript extends ContextScript {


    private static final String loop_param = "L";
    private static final String duration_param = "d";
    private static final String count_param = "c";
    private static final String verbose_param = "v";
    protected boolean loop = false;
    protected Date stopDate = null;
    protected Integer count = null;
    protected int iteration = 0;
    protected boolean verbose;

    public IteratorScript() {
    }

    public void run() throws Exception {
//        try {
//            do {
//                ItemIterator itemIterator = getItemIterator();
//                while (itemIterator.hasNext() && stillActive()) {
//                    Item item = itemIterator.next();
//                    if (verbose) {
//                        print("Processing item " + item.getID());
//                    }
//                    runItem(item);
//                    iteration++;
//                }
//            } while (loop && stillActive());
//            context.complete();
//        } catch (Exception e) {
//            printAndLogError(e);
//        }
    }

    protected boolean stillActive() {
        return (stopDate == null || stopDate.getTime() >= System.currentTimeMillis())
                && (count == null || iteration >= count);
    }

    protected void runItem(Item item) throws Exception {
        print(item.getHandle() + "\t\t" + item.getName());
    }

    @Override
    protected int processLine(CommandLine line) throws org.apache.commons.cli.ParseException {
        int status = super.processLine(line);
        if (status == 0) {
            // other arguments
            setLoop(line.hasOption(loop_param));
            if (line.hasOption(duration_param)) {
                try {
                    setDuration(line.getOptionValue(duration_param));
                } catch (ParseException e) {
                    throw new org.apache.commons.cli.ParseException(e.getMessage());
                }
            }
            if (line.hasOption(count_param)) {
                String optionValue = line.getOptionValue(count_param);
                try {
                    int count = Integer.parseInt(optionValue);
                    setCount(count);
                } catch (NumberFormatException e) {
                    throw new org.apache.commons.cli.ParseException(e.getMessage());
                }
            }
            setVerbose(line.hasOption(verbose_param));
        }
        return status;
    }

    protected Options createCommandLineOptions() {
        Options options = super.createCommandLineOptions();
        options.addOption(loop_param, "loop", false,
                "Loop continuously through items. Default: false");
        options.addOption(duration_param, "duration", true,
                "Max duration this script may run. Default: unlimited");
        options.addOption(count_param, "count", true,
                "Stop after a number of iterations. Default: unlimited");
        options.addOption(verbose_param, "verbose", false, "Verbose output");
        return options;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    private void setDuration(String optionValue) throws ParseException {
        long duration = Utils.parseDuration(optionValue);
        this.stopDate = new Date(System.currentTimeMillis() + duration);
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isVerbose() {
        return verbose;
    }
}
