package com.atmire.scripts.bitstreamchecker;

import com.atmire.scripts.IteratorScript;
import com.atmire.scripts.PrintConsumer;
import com.atmire.utils.Consumer;
import com.atmire.utils.ItemUtils;
import com.atmire.utils.MetadataUtils;
import com.atmire.utils.helper.BitstreamIterator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.handle.HandleManager;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 04 Jul 2016
 */
public class BitstreamChecker extends IteratorScript {

    public static final String bitstream_param = "b";
    public static final String operation_param = "p";
    public static final String item_param = "i";

    private final List<BitstreamCheckOperation> operationList;
    private final int numberPerCommit;

    private List<Integer> includeBitstreams;
    private String[] includeOperations;
    private String item;

    private Date lastScanDate = null;
    private Date startDate = null;

    public BitstreamChecker() {
        ServiceManager serviceManager = new DSpace().getServiceManager();
        //noinspection unchecked
        operationList = serviceManager.getServiceByName("bitstreamCheckerOperations", List.class);
        numberPerCommit = serviceManager.getServiceByName("bitstreamCheckerCommitPerBitstreamAmount", Integer.class);
    }

    public static void main(String[] args) {
        new BitstreamChecker().mainImpl(args);
    }

    @Override
    public void run() throws Exception {
        startDate = new Date();
        print("Started at " + MetadataUtils.getVirusCheckDateFormat().format(startDate));
        try {
            do {
                Iterator<Bitstream> bitstreamIterator = getBitstreamIterator();
                while (bitstreamIterator.hasNext() && stillActive()) {
                    Bitstream bitstream = bitstreamIterator.next();

                    lastScanDate = MetadataUtils.getVirusCheckDate(bitstream);
                    if(stillActive()) {
                        if (verbose) {
                            String lastScanDate = MetadataUtils.getMetadataFirstValue(bitstream, MetadataUtils.virusCheckDateField);
                            print("Processing bitstream " + bitstream.getID() + ", last scan date: " + lastScanDate);
                        }

                        runBitstream(bitstream);
                        bitstream.update();
                        context.removeCached(bitstream, bitstream.getID());

                        iteration++;
                    }
                }
            } while (loop && stillActive());
            context.complete();
        } catch (Exception e) {
            printAndLogError(e);
        }
        print("Ended at " + MetadataUtils.getVirusCheckDateFormat().format(new Date()));
    }

    @Override
    protected boolean stillActive() {
        boolean stillActive = super.stillActive();
        if (!loop && lastScanDate != null) {
            stillActive = startDate.compareTo(lastScanDate) > 0;
        }
        return stillActive;
    }

    private Iterator<Bitstream> getBitstreamIterator() {
        Iterator<Bitstream> bitstreamIterator = null;
        if (CollectionUtils.isNotEmpty(includeBitstreams)) {
            bitstreamIterator = new BitstreamIterator(context, includeBitstreams);
        } else if (StringUtils.isNotBlank(item)) {
            bitstreamIterator = getBitstreamIteratorByItem();
        } else {
            bitstreamIterator = new BitstreamCheckerIterator(context, numberPerCommit);
        }
        return bitstreamIterator;
    }

    private Iterator<Bitstream> getBitstreamIteratorByItem() {
        try {
            Iterator<Bitstream> bitstreamIterator = null;
            Item resolvedItem = null;
            if (item.contains("/")) {
                DSpaceObject dSpaceObject = HandleManager.resolveToObject(context, item);
                if (dSpaceObject instanceof Item) {
                    resolvedItem = (Item) dSpaceObject;
                } else {
                    print(item + " is not an item but a " + dSpaceObject.getTypeText());
                }
            } else {
                // not a handle, maybe an internal ID
                try {
                    resolvedItem = Item.find(context, Integer.valueOf(item));
                } catch (NumberFormatException e) {
                    print(item + " could not be resolved to an item");
                }
            }
            if (resolvedItem != null) {
                bitstreamIterator = ItemUtils.getBitstreams(resolvedItem).iterator();
            }
            return bitstreamIterator;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected void runBitstream(Bitstream bitstream) throws Exception {
        if (CollectionUtils.isNotEmpty(operationList)) {
            for (BitstreamCheckOperation operation : operationList) {
                if (isOperationIncluded(operation)) {
                    if (isVerbose()) {
                        print("Operation: " + operation.getName());
                    }
                    Consumer<String> verbose = isVerbose() ? printer() : noprinter();
                    operation.check(this.context, bitstream, new PrintConsumer(printer(), verbose));
                }
            }
        }
    }

    private boolean isOperationIncluded(BitstreamCheckOperation operation) {
        return ArrayUtils.isEmpty(includeOperations)
                || ArrayUtils.contains(includeOperations, operation.getName());
    }

    @Override
    protected Options createCommandLineOptions() {
        Options options = super.createCommandLineOptions();
        options.addOption(bitstream_param, "bitstream", true,
                "Bitstream ids to include.");
        options.addOption(operation_param, "operations", true,
                "The name of the operations to execute");
        options.addOption(item_param, "item", true,
                "Use the bitstreams of an item. Does not work in combination with -" + bitstream_param);
        return options;
    }

    @Override
    protected int processLine(CommandLine line) throws ParseException {
        int status = super.processLine(line);
        if (status == 0) {
            if (line.hasOption(bitstream_param)) {
                setIncludeBitstreams(line.getOptionValues(bitstream_param));
            }
            if (line.hasOption(operation_param)) {
                setIncludeOperations(line.getOptionValues(operation_param));
            }
            if (line.hasOption(item_param)) {
                setItem(line.getOptionValue(item_param));
            }
        }
        return status;
    }

    public void setIncludeBitstreams(String[] includeBitstreams) {
        this.includeBitstreams = new LinkedList<>();
        for (String includeBitstream : includeBitstreams) {
            int id = Integer.parseInt(includeBitstream);
            this.includeBitstreams.add(id);
        }
    }

    public void setIncludeOperations(String[] includeOperations) {
        this.includeOperations = includeOperations;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getItem() {
        return item;
    }
}
