package com.atmire.utils;

/**
 * Created by dylan on 15/02/16.
 */

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.content.Item;

/**
 * Script to delete all items from dspace. (workspace, workflow, archived).
 *
 */

public class DatabaseCleanScript
{

    private static EPerson eperson; // person running the script.
    private static Logger log = Logger.getLogger(DatabaseCleanScript.class);
    private static Context context;
    public static void main(String[] args)
    {
        try
        {
            CommandLineParser parser = new PosixParser();
            CommandLine line = parser.parse(createCommandLineOptions(),args);
        }
        catch(Exception exception)
        {
            log.error("The DatabaseCleanScript encountered problems when running.",exception);
        }

        DatabaseCleanScript cleanScript = new DatabaseCleanScript();
    }



    private static Options createCommandLineOptions()
    {
        Options options = new Options();

        Option ePerson = OptionBuilder.withArgName("eperson").hasArg().withDescription("EPerson executing this task").isRequired().create("e");
        options.addOption(ePerson);
        options.addOption("h","help",false,"show help");

        return options;
    }

    public DatabaseCleanScript()
    {
        // just call a method to delete all the items.
        deleteAllItems();
    }

    private void deleteAllItems()
    {
        System.out.println("deleting items");
        try
        {
            context = new Context();
            ItemIterator it = Item.findAll(context);
            while(it.hasNext())
            {
                Item item = it.next();
                Collection collection = item.getOwningCollection();
                collection.removeItem(item);
                collection.update();
            }
            context.complete();
        }
        catch(Exception e)
        {
            log.error("During deletion of the items something went wrong.");
        }

    }
}
