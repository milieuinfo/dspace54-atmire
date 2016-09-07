package com.atmire.scripts;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.ArrayUtils;
import org.dspace.content.*;
import org.dspace.handle.HandleManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 21 May 2014
 */
public class ItemIteratorScript extends IteratorScript {


    private static final String identifier_param = "i";
    private static final String omit_param = "o";
    private String[] includes;
    private String[] excludes;

    public ItemIteratorScript() {
    }

    public void run() throws Exception {
        try {
            do {
                ItemIterator itemIterator = getItemIterator();
                while (itemIterator.hasNext() && stillActive()) {
                    Item item = itemIterator.next();
                    if (verbose) {
                        print("Processing item " + item.getID());
                    }
                    runItem(item);
                    iteration++;
                }
            } while (loop && stillActive());
            context.complete();
        } catch (Exception e) {
            printAndLogError(e);
        }
    }

    protected void runItem(Item item) throws Exception {
        print(item.getHandle() + "\t\t" + item.getName());
    }

    public static void main(String[] args) {
        Script Script = new ItemIteratorScript();
        Script.mainImpl(args);
    }

    @Override
    protected int processLine(CommandLine line) throws org.apache.commons.cli.ParseException {
        int status = super.processLine(line);
        if (status == 0) {
            // other arguments
            if (line.hasOption(identifier_param)) {
                setIncludes(line.getOptionValues(identifier_param));
            }
            if (line.hasOption(omit_param)) {
                setExcludes(line.getOptionValues(omit_param));
            }
        }
        return status;
    }

    protected Options createCommandLineOptions() {
        Options options = super.createCommandLineOptions();
        options.addOption(identifier_param, "identifier", true,
                "Items, collections, communities over which to iterate. Default: all archived items");
        options.addOption(omit_param, "omit", true,
                "Items, collections, communities to exclude. Default: none");
        return options;
    }

    protected ItemIterator getItemIterator() throws SQLException {
        ItemIterator items = null;

        if (ArrayUtils.isNotEmpty(includes)) {
            Set<Integer> ids = getItemIDs(includes);
            items = new ItemIterator(context, new ArrayList<>(ids));
        }

        if (ArrayUtils.isEmpty(includes) || items == null || !items.hasNext()) {
            items = Item.findAll(context);
        }

        if (ArrayUtils.isNotEmpty(excludes)) {
            Set<Integer> excludedIDs = getItemIDs(excludes);
            Set<Integer> ids = new HashSet<>();
            while (items.hasNext()) {
                Item item = items.next();
                int id = item.getID();
                if (excludedIDs == null || !excludedIDs.contains(id)) {
                    ids.add(id);
                }
            }
            items = new ItemIterator(context, new ArrayList<>(ids));
        }
        return items;
    }

    /**
     * Returns a set of all item IDs present in "includes"
     * and all items from a collection or community present in "includes".
     *
     * @param includes An array of handles or item IDs
     * @throws SQLException
     */
    protected Set<Integer> getItemIDs(String[] includes) throws SQLException {
        Set<Integer> ids = new HashSet<>();
        if (includes != null) {
            for (String handle : includes) {
                if (handle.contains("/")) {
                    DSpaceObject dSpaceObject = HandleManager.resolveToObject(context, handle);
                    if (dSpaceObject instanceof Item) {
                        ids.add(dSpaceObject.getID());
                    } else if (dSpaceObject instanceof Collection) {
                        Collection collection = (Collection) dSpaceObject;
                        addCollection(ids, collection);
                    } else if (dSpaceObject instanceof Community) {
                        Community community = (Community) dSpaceObject;
                        addCommunity(ids, community);
                    } else {
                        print(handle + " could not be resolved to an item, collection or community");
                    }
                } else {
                    // not a handle, maybe an internal ID
                    Item item = null;
                    try {
                        item = Item.find(context, Integer.valueOf(handle));
                    } catch (NumberFormatException e) {
                        print(handle + " could not be resolved to an item");
                    }

                    if (item != null) {
                        ids.add(item.getID());
                    }
                }
            }
        }
        return ids;
    }

    protected void addCommunity(Set<Integer> ids, Community community) throws SQLException {
        Community[] subcommunities = community.getSubcommunities();
        for (Community subcommunity : subcommunities) {
            addCommunity(ids, subcommunity);
        }
        Collection[] collections = community.getCollections();
        for (Collection collection : collections) {
            addCollection(ids, collection);
        }
    }

    protected void addCollection(Set<Integer> ids, Collection collection) throws SQLException {
        ItemIterator collectionItems = collection.getItems();
        while (collectionItems.hasNext()) {
            Item item = collectionItems.next();
            ids.add(item.getID());
            item.decache();
        }
    }

    public void setIncludes(String[] includes) {
        this.includes = includes;
    }

    public void setExcludes(String[] excludes) {
        this.excludes = excludes;
    }
}
