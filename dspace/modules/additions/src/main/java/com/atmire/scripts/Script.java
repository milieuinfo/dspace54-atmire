package com.atmire.scripts;

import com.atmire.utils.Consumer;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.ISO8601DateFormat;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 21 May 2014
 */
public class Script {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(Script.class);

    protected PrintWriter print = null;

    protected boolean printToLog = false;

    public Script() {
        print = new PrintWriter(System.out, true);
    }

    public static void main(String[] args) {

        Script Script = new Script();
        Script.mainImpl(args);
    }

    protected void mainImpl(String[] args) {
        try {
            if (processArgs(args) == 0) {
                System.exit(0);
            }
            run();

        } catch (ParseException e) {
            print(e.getMessage());
            printHelp(createCommandLineOptions());
        } catch (Exception e) {
            printAndLogError(e);
        }
    }

    protected int processArgs(String[] args) throws ParseException {
        CommandLineParser parser = new PosixParser();
        Options options = createCommandLineOptions();
        CommandLine line = parser.parse(options, args);

        // help
        if (line.hasOption("h")) {
            printHelp(options);
            return 0;
        }

        // other arguments
        int status = processLine(line);
        if (status != 0) {
            return status;
        }

        // print to std out
        setPrinter(new PrintWriter(System.out, true));
        return 1;
    }

    private void printHelp(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("dsrun " + getClass().getCanonicalName(), options);
    }

    /**
     * Hook for subclasses
     */
    protected int processLine(CommandLine line) throws ParseException {
        return 0;
    }

    protected Options createCommandLineOptions() {
        Options options = new Options();
        options.addOption("h", "help", false, "Prints a helpful message about this script's usage");

        /** Example - long
         OptionBuilder.withDescription("File to import");
         OptionBuilder.withLongOpt("file");
         OptionBuilder.hasArg(true);
         OptionBuilder.isRequired(true);
         options.addOption(OptionBuilder.create("f"));
         */
        return options;
    }

    /**
     * The actual functionality of the script.
     */
    public void run() throws Exception {
        print("Script initializing...");
        try {
            print.println("Running wild !");
        } catch (Exception e) {
            printAndLogError(e);
        }
        print("Script done.");
    }


    public void print(String line) {
        if (printToLog) {
            log.info(line);
        } else {
            print.println(line);
        }
    }

    public void printAndLogError(Exception error) {
        Date date = new Date();
        DateFormat dateFormat = new ISO8601DateFormat();
        String message = "Error in " + getClass().getCanonicalName() + " " + error.getClass() + " at " + dateFormat.format(date);
        if(print!=null) {
            print.println(message);
            error.printStackTrace(print);
        }
        log.error(message, error);
    }

    public void setPrinter(PrintWriter print) {
        this.print = print;
    }

    public void setPrintToLog(boolean printToLog) {
        this.printToLog = printToLog;
    }

    protected Consumer<String> printer() {
        return new Consumer<String>() {
            @Override
            public void consume(String s) {
                print(s);
            }
        };
    }

    protected Consumer<String> noprinter() {
        return new Consumer<String>() {
            @Override
            public void consume(String s) {
            }
        };
    }
}
