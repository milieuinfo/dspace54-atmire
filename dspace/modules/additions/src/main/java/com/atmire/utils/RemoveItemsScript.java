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
 * Script to delete all items from dspace.
 *
 */

public class RemoveItemsScript
{
    private static EPerson eperson; // person running the script.
    private static Logger log = Logger.getLogger(RemoveItemsScript.class);
    private static Context context;
    public static void main(String[] args)
    {
        try
        {
            context = new Context();
            CommandLineParser parser = new PosixParser();
            CommandLine line = parser.parse(createCommandLineOptions(),args);

            String EPersonMail = line.hasOption("e") ? line.getOptionValue("e") : null;
            eperson = EPersonMail.indexOf("@") != -1 ? EPerson.findByEmail(context,EPersonMail) : EPerson.find(context, Integer.parseInt(EPersonMail));
            context.setCurrentUser(eperson);
        }
        catch(Exception exception)
        {
            log.error("The remove-item script encountered problems whilst running.",exception);
        }

        RemoveItemsScript cleanScript = new RemoveItemsScript();
        System.out.println("Items deleted");
    }



    private static Options createCommandLineOptions()
    {
        Options options = new Options();

        Option ePerson = OptionBuilder.withArgName("eperson").hasArg().withDescription("EPerson executing this task").isRequired().create("e");
        options.addOption(ePerson);
        options.addOption("h", "help", false, "show help");

        return options;
    }

    public RemoveItemsScript()
    {
        // just call a method to delete all the items.
        deleteAllItems();
    }

    private void deleteAllItems()
    {
        System.out.println("deleting items");
        try
        {
            ItemIterator it = Item.findAll(context);
            while(it.hasNext())
            {
                Item item = it.next();
                Collection collection = item.getOwningCollection();
                collection.removeItem(item);
                collection.update();
            }
            context.commit();
        }
        catch(Exception e)
        {
            log.error("During deletion of the items something went wrong.");
        }
        finally
        {
            try
            {
                context.complete();
            }
            catch(Exception ex)
            {
                log.error("An error occurred when trying to finalize the current operation.");
            }
        }

    }
}
