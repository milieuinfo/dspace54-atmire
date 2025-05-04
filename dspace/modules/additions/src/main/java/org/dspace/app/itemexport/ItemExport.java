/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemexport;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.*;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.elasticsearch.common.jackson.dataformat.yaml.snakeyaml.scanner.Constant;

import javax.mail.MessagingException;
import java.io.*;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Item exporter to create simple AIPs for DSpace content. Currently exports
 * individual items, or entire collections. For instructions on use, see
 * printUsage() method.
 * <P>
 * ItemExport creates the simple AIP package that the importer also uses. It
 * consists of:
 * <P>
 * /exportdir/42/ (one directory per item) / dublin_core.xml - qualified dublin
 * core in RDF schema / contents - text file, listing one file per line / file1
 * - files contained in the item / file2 / ...
 * <P>
 * issues -doesn't handle special characters in metadata (needs to turn &'s into
 * &amp;, etc.)
 * <P>
 * Modified by David Little, UCSD Libraries 12/21/04 to allow the registration
 * of files (bitstreams) into DSpace.
 *
 * @author David Little
 * @author Jay Paz
 */
public class ItemExport
{
    private boolean zip = false;
    private boolean updatedItemsOnly;
    private boolean migrate = false;
    private boolean handleBasedDirectoryStructure = false;

    private int seqStart = -1;
    private int myType = -1;

    private String typeString = null;
    private String destDirName = null;
    private String myIDString = null;
    private String zipFileName = "";

    private Collection mycollection = null;
    private Item myItem = null;

    private DateFile dateFile;

    /**
     * used for export download
     */
    public static final String COMPRESSED_EXPORT_MIME_TYPE = "application/zip";

    /** log4j logger */
    private static Logger log = Logger.getLogger(ItemExport.class);

    public static void main(String[] argv) throws Exception {
        ItemExport itemExport = new ItemExport();
        itemExport.mainImpl(argv);
    }

    private void mainImpl(String[] argv) throws Exception{

        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = createOptions();

        CommandLine line = parser.parse(options, argv);


        handleBasedDirectoryStructure = line.hasOption('g');
        updatedItemsOnly = line.hasOption('c');


        parseCommandLineOptions(options, line);
        validateArguments();


        Context c = new Context();
        c.turnOffAuthorisationSystem();

        if (myType == Constants.ITEM)
        {
            // first, is myIDString a handle?
            if (myIDString.indexOf('/') != -1)
            {
                myItem = (Item) HandleManager.resolveToObject(c, myIDString);

                if ((myItem == null) || (myItem.getType() != Constants.ITEM))
                {
                    myItem = null;
                }
            }
            else
            {
                myItem = Item.find(c, Integer.parseInt(myIDString));
            }

            if (myItem == null)
            {
                logAndPrintMessage("Error, item cannot be found: " + myIDString, Level.ERROR);
            }
        }
        else if(myType == Constants.COLLECTION)
        {
            if (myIDString.indexOf('/') != -1)
            {
                // has a / must be a handle
                mycollection = (Collection) HandleManager.resolveToObject(c, myIDString);

                // ensure it's a collection
                if ((mycollection == null)
                        || (mycollection.getType() != Constants.COLLECTION))
                {
                    mycollection = null;
                }
            }
            else if (myIDString != null)
            {
                mycollection = Collection.find(c, Integer.parseInt(myIDString));
            }

            if (mycollection == null)
            {
                logAndPrintMessage("Error, collection cannot be found: " + myIDString, Level.ERROR);
                System.exit(1);
            }
        }

        if (updatedItemsOnly) {
            dateFile = new DateFileBasic(destDirName);
        } else {
            dateFile = new DateFileNoop();
        }
        boolean exportFailed = false;
        String adminMail = ConfigurationManager.getProperty("mail.admin");

        try {
            exportItems(c);
        } catch (Throwable t) {
            sendErrorMail(adminMail, t);
            logAndPrintError("Something went wrong when exporting the items: " + t.getMessage(), t);
            exportFailed=true;
        }

        if(exportFailed){
            c.abort();
        } else {
            sendSuccessMail(adminMail);
            c.complete();
            dateFile.writeDate();
        }
    }

    private void sendErrorMail(String adminMail, Throwable t) throws IOException, MessagingException {
        if(updatedItemsOnly) {
            Email email = Email.getEmail(I18nUtil.getEmailFilename(I18nUtil.getDefaultLocale(), "export_error_admin"));
            email.addRecipient(adminMail);
            email.addArgument(t.getMessage());
            email.send();
        }
    }

    private void sendSuccessMail(String adminMail) throws IOException, MessagingException {
        if(updatedItemsOnly) {
            Email email = Email.getEmail(I18nUtil.getEmailFilename(I18nUtil.getDefaultLocale(), "export_success_admin"));
            email.addRecipient(adminMail);
            email.addArgument(destDirName);
            email.send();
        }
    }

    private static void logAndPrintMessage(String message, Priority priority){
        System.out.println(message);
        log.log(priority, message);
    }

    private static void logAndPrintError(final String message, final Throwable t) {
        System.err.println(message);
        log.error(message, t);
    }

    private void validateArguments() {
        // now validate the args
        if (myType == -1)
        {
            System.out.println("type must be either COLLECTION, ITEM or SITE (-h for help)");
            System.exit(1);
        }

        if (destDirName == null)
        {
            System.out.println("destination directory must be set (-h for help)");
            System.exit(1);
        }

        if (!handleBasedDirectoryStructure && seqStart == -1)
        {
            System.out.println("sequence start number must be set (-h for help)");
            System.exit(1);
        }

        if (myIDString == null && myType != Constants.SITE)
        {
            System.out.println("ID must be set to either a database ID or a handle (-h for help)");
            System.exit(1);
        }
    }

    private void parseCommandLineOptions(Options options, CommandLine line) {
        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ItemExport\n", options);
            System.out.println("\nfull collection: ItemExport -t COLLECTION -i ID -d dest -n number");
            System.out.println("singleitem:       ItemExport -t ITEM -i ID -d dest -n number");

            System.exit(0);
        }

        if (line.hasOption('t')) // type
        {
            typeString = line.getOptionValue('t');

            if ("ITEM".equals(typeString))
            {
                myType = Constants.ITEM;
            }
            else if ("COLLECTION".equals(typeString))
            {
                myType = Constants.COLLECTION;
            }
            else if("SITE".equals(typeString)){
                myType = Constants.SITE;
            }
        }

        if (line.hasOption('i')) // id
        {
            myIDString = line.getOptionValue('i');
        }

        if (line.hasOption('d')) // dest
        {
            destDirName = line.getOptionValue('d');
        }

        if (line.hasOption('n')) // number
        {
            seqStart = Integer.parseInt(line.getOptionValue('n'));
        } else {
            if (!handleBasedDirectoryStructure) {
                System.out.println("The -n parameter is required when the -g parameter is not provided.");
                System.exit(0);
            }
        }

        if (line.hasOption('m')) // number
        {
            migrate = true;
        }

        if (line.hasOption('z'))
        {
            zip = true;
            zipFileName = line.getOptionValue('z');
        }
    }

    private Options createOptions() {
        Options options = new Options();

        options.addOption("t", "type", true, "type: COLLECTION, ITEM or SITE");
        options.addOption("i", "id", true, "ID or handle of thing to export");
        options.addOption("d", "dest", true, "destination where you want items to go");
        options.addOption("m", "migrate", false, "export for migration (remove handle and metadata that will be re-created in new system)");
        options.addOption("n", "number", true, "sequence number to begin exporting items with. optional when with -g, otherwise mandatory");
        options.addOption("z", "zip", true, "export as zip file (specify filename e.g. export.zip)");
        options.addOption("h", "help", false, "help");
        options.addOption("g", "generate-directory-structure", false, "Generate directory structure based on handle");
        options.addOption("c", "updated-items-only", false, "Only exports items that have been updated since the last time this option was used");
        return options;
    }


    private void exportItems(Context c) throws Exception {
        if (zip)
        {
            ItemIterator items;
            if (myItem != null)
            {
                List<Integer> myItems = new ArrayList<>();
                myItems.add(myItem.getID());
                items = new ItemIterator(c, myItems);
            }
            else if(mycollection != null)
            {
                logAndPrintMessage("Exporting from collection: " + myIDString,Level.INFO);
                items = getItemsIterator(mycollection, dateFile, c);
            }
            else {
                logAndPrintMessage("Exporting all items",Level.INFO);
                items = getAllItemsIterator(dateFile, c);
            }
            exportAsZip(c, items, destDirName, zipFileName, seqStart, migrate, handleBasedDirectoryStructure);
        }
        else
        {
            if (myItem != null)
            {
                // it's only a single item
                exportItem(c, myItem, destDirName, seqStart, migrate, handleBasedDirectoryStructure);
            }
            else if (mycollection != null)
            {
                logAndPrintMessage("Exporting from collection: " + myIDString, Level.INFO);

                // it's a collection, so do a bunch of items
                ItemIterator i = getItemsIterator(mycollection, dateFile, c);
                try
                {
                    exportItem(c, i, destDirName, seqStart, migrate, handleBasedDirectoryStructure);
                }
                finally
                {
                    if (i != null)
                    {
                        i.close();
                    }
                }
            }
            else {
                logAndPrintMessage("Exporting all items", Level.INFO);

                // it's a collection, so do a bunch of items
                ItemIterator i = getAllItemsIterator(dateFile, c);

                try
                {
                    exportItem(c, i, destDirName, seqStart, migrate, handleBasedDirectoryStructure);
                }
                finally
                {
                    i.close();
                }
            }
        }
    }

    private static ItemIterator getItemsIterator(Collection mycollection, DateFile dateFile, Context context)
            throws SQLException {
        ItemIterator items;
        Date lastDate = dateFile.getLastDate();
        if (lastDate != DateFile.NO_DATE) {
            items = getItemsIterator(mycollection, lastDate, context);
        } else {
            items = mycollection.getItems();
        }
        return items;
    }

    private static ItemIterator getItemsIterator(Collection mycollection, Date lastDate, Context context) {
            String myQuery = "SELECT item.* FROM item, collection2item WHERE "
                    + "item.item_id=collection2item.item_id AND "
                    + "collection2item.collection_id= ? "
                    + "AND item.in_archive='1' "
                    + "AND item.last_modified > ? ";

        TableRowIterator rows;
        try {
            rows = DatabaseManager.queryTable(context, "item", myQuery, mycollection.getID(), new Timestamp(lastDate.getTime()));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return new ItemIterator(context, rows);
    }

    private static ItemIterator getAllItemsIterator(DateFile dateFile, Context context)
        throws SQLException {
        ItemIterator items;
        Date lastDate = dateFile.getLastDate();
        if (lastDate != DateFile.NO_DATE) {
            items = getAllItemsIterator(lastDate, context);
        } else {
            items = getAllItemsIterator(context);
        }
        return items;
    }

    private static ItemIterator getAllItemsIterator(Date lastDate, Context context) {
        String myQuery = "SELECT item.* FROM item WHERE item.in_archive='1' AND item.last_modified > ? ";

        TableRowIterator rows;
        try {
            rows = DatabaseManager.queryTable(context, "item", myQuery, new Timestamp(lastDate.getTime()));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return new ItemIterator(context, rows);
    }

    private static ItemIterator getAllItemsIterator(Context context) {
        String myQuery = "SELECT item.* FROM item WHERE item.in_archive='1' ";

        TableRowIterator rows;
        try {
            rows = DatabaseManager.queryTable(context, "item", myQuery);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return new ItemIterator(context, rows);
    }

    private static void exportItem(Context c, ItemIterator i, String destDirName, int seqStart, boolean migrate, boolean handleBasedDirectoryStructure)
            throws Exception {
        int mySequenceNumber = seqStart;
        String fullPath = destDirName;


        logAndPrintMessage("Beginning export" ,Level.INFO);

        while (i.hasNext()) {

            Item next = i.next();

            if (handleBasedDirectoryStructure) {
                String subdir = new Subdir().getSubDir(next);
                fullPath = destDirName + File.separator + subdir;
            }

            File directory = new File(fullPath);
            if (handleBasedDirectoryStructure && directory.exists()) {
                logAndPrintMessage(fullPath + " exists. Removing it.", Level.INFO);
                FileUtils.deleteDirectory(directory);
            }

            if (!directory.mkdirs()) {
                logAndPrintMessage("Cannot create directories at " + fullPath,Level.ERROR);
                throw new IOException("Cannot create directories at "+ fullPath+", please verify the required permissions at this location.");
            } else {
                logAndPrintMessage("Exporting item to " + fullPath, Level.INFO);

                exportItem(c, next, fullPath, mySequenceNumber, migrate, handleBasedDirectoryStructure);
                mySequenceNumber++;
            }
        }

        logAndPrintMessage("Export finished without problems", Level.INFO);
    }



    private static void exportItem(Context c, Item myItem, String destDirName,
                                   int seqStart, boolean migrate, boolean handleBasedDirectoryStructure) throws Exception
    {
        // now create a subdirectory
        if(seqStart==-1){
            seqStart++;
        }
        File destDir = new File(destDirName);

        if (destDir.exists())
        {

            File itemDir = null;
            if(handleBasedDirectoryStructure) {
                itemDir = new File(destDir + "/item");
            } else {
                itemDir = new File(destDir + "/" + seqStart);
            }

            if (itemDir.exists()) {
                logAndPrintMessage(itemDir.getAbsolutePath() + " exists. Removing it.", Level.INFO);
                FileUtils.deleteDirectory(itemDir);
            }

            logAndPrintMessage("Exporting Item " + myItem.getID() + " to " + itemDir,Level.INFO);

            if (itemDir.mkdirs())
            {
                // make it this far, now start exporting
                writeMetadata(c, myItem, itemDir, migrate);
                writeBitstreams(c, myItem, itemDir);
                if (!migrate)
                {
                    writeHandle(c, myItem, itemDir);
                }
            }
            else
            {
                throw new Exception("Error, can't make dir " + itemDir);
            }
        }
        else
        {
            throw new Exception("Error, directory " + destDirName + " doesn't exist!");
        }
        if(seqStart % 100 == 0) {
            c.clearCache();
        }
    }

    /**
     * Discover the different schemas in use and output a separate metadata XML
     * file for each schema.
     *
     * @param c
     * @param i
     * @param destDir
     * @throws Exception
     */
    private static void writeMetadata(Context c, Item i, File destDir, boolean migrate)
            throws Exception
    {
        Set<String> schemas = new HashSet<String>();
        Metadatum[] dcValues = i.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (Metadatum dcValue : dcValues)
        {
            schemas.add(dcValue.schema);
        }

        // Save each of the schemas into it's own metadata file
        for (String schema : schemas)
        {
            writeMetadata(c, schema, i, destDir, migrate);
        }
    }

    // output the item's dublin core into the item directory
    private static void writeMetadata(Context c, String schema, Item i,
                                      File destDir, boolean migrate) throws Exception
    {
        String filename;
        if (schema.equals(MetadataSchema.DC_SCHEMA))
        {
            filename = "dublin_core.xml";
        }
        else
        {
            filename = "metadata_" + schema + ".xml";
        }

        File outFile = new File(destDir, filename);

        logAndPrintMessage("Attempting to create file " + outFile,Level.INFO);


        if (outFile.createNewFile())
        {
            BufferedOutputStream out = new BufferedOutputStream(
                    new FileOutputStream(outFile));

            Metadatum[] dcorevalues = i.getMetadata(schema, Item.ANY, Item.ANY,
                    Item.ANY);

            // XML preamble
            byte[] utf8 = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n"
                    .getBytes("UTF-8");
            out.write(utf8, 0, utf8.length);

            String dcTag = "<dublin_core schema=\"" + schema + "\">\n";
            utf8 = dcTag.getBytes("UTF-8");
            out.write(utf8, 0, utf8.length);

            String dateIssued = null;
            String dateAccessioned = null;

            for (Metadatum dcv : dcorevalues)
            {
                String qualifier = dcv.qualifier;

                if (qualifier == null)
                {
                    qualifier = "none";
                }

                String language = dcv.language;

                if (language != null)
                {
                    language = " language=\"" + language + "\"";
                }
                else
                {
                    language = "";
                }

                utf8 = ("  <dcvalue element=\"" + dcv.element + "\" "
                        + "qualifier=\"" + qualifier + "\""
                        + language + ">"
                        + Utils.addEntities(dcv.value) + "</dcvalue>\n")
                        .getBytes("UTF-8");

                if ((!migrate) ||
                        (migrate && !(
                                ("date".equals(dcv.element) && "issued".equals(qualifier)) ||
                                        ("date".equals(dcv.element) && "accessioned".equals(qualifier)) ||
                                        ("date".equals(dcv.element) && "available".equals(qualifier)) ||
                                        ("identifier".equals(dcv.element) && "uri".equals(qualifier) &&
                                                (dcv.value != null && dcv.value.startsWith("http://hdl.handle.net/" +
                                                        HandleManager.getPrefix() + "/"))) ||
                                        ("description".equals(dcv.element) && "provenance".equals(qualifier)) ||
                                        ("format".equals(dcv.element) && "extent".equals(qualifier)) ||
                                        ("format".equals(dcv.element) && "mimetype".equals(qualifier)))))
                {
                    out.write(utf8, 0, utf8.length);
                }

                // Store the date issued and accession to see if they are different
                // because we need to keep date.issued if they are, when migrating
                if (("date".equals(dcv.element) && "issued".equals(qualifier)))
                {
                    dateIssued = dcv.value;
                }
                if (("date".equals(dcv.element) && "accessioned".equals(qualifier)))
                {
                    dateAccessioned = dcv.value;
                }
            }

            // When migrating, only keep date.issued if it is different to date.accessioned
            if ((migrate) &&
                    (dateIssued != null) &&
                    (dateAccessioned != null) &&
                    (!dateIssued.equals(dateAccessioned)))
            {
                utf8 = ("  <dcvalue element=\"date\" "
                        + "qualifier=\"issued\">"
                        + Utils.addEntities(dateIssued) + "</dcvalue>\n")
                        .getBytes("UTF-8");
                out.write(utf8, 0, utf8.length);
            }

            utf8 = "</dublin_core>\n".getBytes("UTF-8");
            out.write(utf8, 0, utf8.length);

            out.close();
        }
        else
        {
            throw new Exception("Cannot create dublin_core.xml in " + destDir);
        }
    }

    // create the file 'handle' which contains the handle assigned to the item
    private static void writeHandle(Context c, Item i, File destDir)
            throws Exception
    {
        if (i.getHandle() == null)
        {
            return;
        }
        String filename = "handle";

        File outFile = new File(destDir, filename);

        if (outFile.createNewFile())
        {
            PrintWriter out = new PrintWriter(new FileWriter(outFile));

            out.println(i.getHandle());

            // close the contents file
            out.close();
        }
        else
        {
            throw new Exception("Cannot create file " + filename + " in "
                    + destDir);
        }
    }

    /**
     * Create both the bitstreams and the contents file. Any bitstreams that
     * were originally registered will be marked in the contents file as such.
     * However, the export directory will contain actual copies of the content
     * files being exported.
     *
     * @param c
     *            the DSpace context
     * @param i
     *            the item being exported
     * @param destDir
     *            the item's export directory
     * @throws Exception
     *             if there is any problem writing to the export directory
     */
    private static void writeBitstreams(Context c, Item i, File destDir)
            throws Exception
    {
        File outFile = new File(destDir, "contents");

        if (outFile.createNewFile())
        {
            PrintWriter out = new PrintWriter(new FileWriter(outFile));

            Bundle[] bundles = i.getBundles();

            for (int j = 0; j < bundles.length; j++)
            {
                // bundles can have multiple bitstreams now...
                Bundle currentBundle = bundles[j];
                Bitstream[] bitstreams = currentBundle.getBitstreams();

                String bundleName = currentBundle.getName();

                for (int k = 0; k < bitstreams.length; k++)
                {
                    Bitstream b = bitstreams[k];

                    String myName = b.getName();
                    String oldName = myName;

                    String description = b.getDescription();
                    if (!StringUtils.isEmpty(description))
                    {
                        description = "\tdescription:" + description;
                    } else
                    {
                        description = "";
                    }

                    String primary = "";
                    if (currentBundle.getPrimaryBitstreamID() == b.getID()) {
                        primary = "\tprimary:true ";
                    }

                    int myPrefix = 1; // only used with name conflict

                    boolean isDone = false; // done when bitstream is finally written
                    InputStream is = null;
                    try {
                        is = b.retrieve();
                    } catch (FileNotFoundException ex) {
                        log.warn("File not found exception, excluding this bitstream from the export: " + ex.getMessage());
                        isDone = true;
                    }

                    while (!isDone)
                    {
                        if (myName.contains(File.separator))
                        {
                            String dirs = myName.substring(0, myName
                                    .lastIndexOf(File.separator));
                            File fdirs = new File(destDir + File.separator
                                    + dirs);
                            if (!fdirs.exists() && !fdirs.mkdirs())
                            {
                                log.error("Unable to create destination directory");
                            }
                        }

                        File fout = new File(destDir, myName);

                        if (fout.createNewFile())
                        {
                            FileOutputStream fos = new FileOutputStream(fout);
                            Utils.bufferedCopy(is, fos);
                            // close streams
                            is.close();
                            fos.close();

                            // write the manifest file entry
                            if (b.isRegisteredBitstream())
                            {
                                out.println("-r -s " + b.getStoreNumber()
                                        + " -f " + myName +
                                        "\tbundle:" + bundleName +
                                        primary + description);
                            }
                            else
                            {
                                out.println(myName + "\tbundle:" + bundleName +
                                        primary + description);
                            }

                            isDone = true;
                        }
                        else
                        {
                            myName = myPrefix + "_" + oldName; // keep
                            // appending
                            // numbers to the
                            // filename until
                            // unique
                            myPrefix++;
                        }
                    }
                }
            }

            // close the contents file
            out.close();
        }
        else
        {
            throw new Exception("Cannot create contents in " + destDir);
        }
    }

    /**
     * Method to perform an export and save it as a zip file.
     *
     * @param context The DSpace Context
     * @param items The items to export
     * @param destDirName The directory to save the export in
     * @param zipFileName The name to save the zip file as
     * @param seqStart The first number in the sequence
     * @param migrate Whether to use the migrate option or not
     * @throws Exception
     */
    public static void exportAsZip(Context context, ItemIterator items,
                                   String destDirName, String zipFileName,
                                   int seqStart, boolean migrate, boolean handleBasedDirectoryStructure) throws Exception
    {
        String workDir = getExportWorkDirectory() +
                System.getProperty("file.separator") +
                zipFileName;

        File wkDir = new File(workDir);
        if (!wkDir.exists() && !wkDir.mkdirs())
        {
            log.error("Unable to create working direcory");
        }

        File dnDir = new File(destDirName);
        if (!dnDir.exists() && !dnDir.mkdirs())
        {
            log.error("Unable to create destination directory");
        }

        // export the items using normal export method
        exportItem(context, items, workDir, seqStart, migrate, handleBasedDirectoryStructure);

        // now zip up the export directory created above
        zip(workDir, destDirName + System.getProperty("file.separator") + zipFileName);
    }

    /**
     * Convenience methot to create export a single Community, Collection, or
     * Item
     *
     * @param dso
     *            - the dspace object to export
     * @param context
     *            - the dspace context
     * @throws Exception
     */
    public static void createDownloadableExport(DSpaceObject dso,
                                                Context context, boolean migrate) throws Exception
    {
        EPerson eperson = context.getCurrentUser();
        ArrayList<DSpaceObject> list = new ArrayList<DSpaceObject>(1);
        list.add(dso);
        processDownloadableExport(list, context, eperson == null ? null
                : eperson.getEmail(), migrate);
    }

    /**
     * Convenience method to export a List of dspace objects (Community,
     * Collection or Item)
     *
     * @param dsObjects
     *            - List containing dspace objects
     * @param context
     *            - the dspace context
     * @throws Exception
     */
    public static void createDownloadableExport(List<DSpaceObject> dsObjects,
                                                Context context, boolean migrate) throws Exception
    {
        EPerson eperson = context.getCurrentUser();
        processDownloadableExport(dsObjects, context, eperson == null ? null
                : eperson.getEmail(), migrate);
    }

    /**
     * Convenience methot to create export a single Community, Collection, or
     * Item
     *
     * @param dso
     *            - the dspace object to export
     * @param context
     *            - the dspace context
     * @param additionalEmail
     *            - cc email to use
     * @throws Exception
     */
    public static void createDownloadableExport(DSpaceObject dso,
                                                Context context, String additionalEmail, boolean migrate) throws Exception
    {
        ArrayList<DSpaceObject> list = new ArrayList<DSpaceObject>(1);
        list.add(dso);
        processDownloadableExport(list, context, additionalEmail, migrate);
    }

    /**
     * Convenience method to export a List of dspace objects (Community,
     * Collection or Item)
     *
     * @param dsObjects
     *            - List containing dspace objects
     * @param context
     *            - the dspace context
     * @param additionalEmail
     *            - cc email to use
     * @throws Exception
     */
    public static void createDownloadableExport(List<DSpaceObject> dsObjects,
                                                Context context, String additionalEmail, boolean migrate) throws Exception
    {
        processDownloadableExport(dsObjects, context, additionalEmail, migrate);
    }

    /**
     * Does the work creating a List with all the Items in the Community or
     * Collection It then kicks off a new Thread to export the items, zip the
     * export directory and send confirmation email
     *
     * @param dsObjects
     *            - List of dspace objects to process
     * @param context
     *            - the dspace context
     * @param additionalEmail
     *            - email address to cc in addition the the current user email
     * @throws Exception
     */
    private static void processDownloadableExport(List<DSpaceObject> dsObjects,
                                                  Context context, final String additionalEmail, boolean toMigrate) throws Exception
    {
        final EPerson eperson = context.getCurrentUser();
        final boolean migrate = toMigrate;

        // before we create a new export archive lets delete the 'expired'
        // archives
        //deleteOldExportArchives(eperson.getID());
        deleteOldExportArchives();

        // keep track of the commulative size of all bitstreams in each of the
        // items
        // it will be checked against the config file entry
        double size = 0;
        final HashMap<String, List<Integer>> itemsMap = new HashMap<String, List<Integer>>();
        for (DSpaceObject dso : dsObjects)
        {
            if (dso.getType() == Constants.COMMUNITY)
            {
                Community community = (Community) dso;
                // get all the collections in the community
                Collection[] collections = community.getAllCollections();
                for (Collection collection : collections)
                {
                    ArrayList<Integer> items = new ArrayList<Integer>();
                    // get all the items in each collection
                    ItemIterator iitems = collection.getItems();
                    try
                    {
                        while (iitems.hasNext())
                        {
                            Item item = iitems.next();
                            // get all the bundles in the item
                            Bundle[] bundles = item.getBundles();
                            for (Bundle bundle : bundles)
                            {
                                // get all the bitstreams in each bundle
                                Bitstream[] bitstreams = bundle.getBitstreams();
                                for (Bitstream bit : bitstreams)
                                {
                                    // add up the size
                                    size += bit.getSize();
                                }
                            }
                            items.add(item.getID());
                        }
                    }
                    finally
                    {
                        if (iitems != null)
                        {
                            iitems.close();
                        }
                        if (items.size() > 0)
                        {
                            itemsMap.put("collection_"+collection.getID(), items);
                        }
                    }
                }
            }
            else if (dso.getType() == Constants.COLLECTION)
            {
                Collection collection = (Collection) dso;
                ArrayList<Integer> items = new ArrayList<Integer>();

                // get all the items in the collection
                ItemIterator iitems = collection.getItems();
                try
                {
                    while (iitems.hasNext())
                    {
                        Item item = iitems.next();
                        // get all thebundles in the item
                        Bundle[] bundles = item.getBundles();
                        for (Bundle bundle : bundles)
                        {
                            // get all the bitstreams in the bundle
                            Bitstream[] bitstreams = bundle.getBitstreams();
                            for (Bitstream bit : bitstreams)
                            {
                                // add up the size
                                size += bit.getSize();
                            }
                        }
                        items.add(item.getID());
                    }
                }
                finally
                {
                    if (iitems != null)
                    {
                        iitems.close();
                    }
                    if (items.size() > 0)
                    {
                        itemsMap.put("collection_"+collection.getID(), items);
                    }
                }
            }
            else if (dso.getType() == Constants.ITEM)
            {
                Item item = (Item) dso;
                // get all the bundles in the item
                Bundle[] bundles = item.getBundles();
                for (Bundle bundle : bundles)
                {
                    // get all the bitstreams in the bundle
                    Bitstream[] bitstreams = bundle.getBitstreams();
                    for (Bitstream bit : bitstreams)
                    {
                        // add up the size
                        size += bit.getSize();
                    }
                }
                ArrayList<Integer> items = new ArrayList<Integer>();
                items.add(item.getID());
                itemsMap.put("item_"+item.getID(), items);
            }
            else
            {
                // nothing to do just ignore this type of DSpaceObject
            }
        }

        // check the size of all the bitstreams against the configuration file
        // entry if it exists
        String megaBytes = ConfigurationManager
                .getProperty("org.dspace.app.itemexport.max.size");
        if (megaBytes != null)
        {
            float maxSize = 0;
            try
            {
                maxSize = Float.parseFloat(megaBytes);
            }
            catch (Exception e)
            {
                // ignore...configuration entry may not be present
            }

            if (maxSize > 0 && maxSize < (size / 1048576.00))
            { // a megabyte
                throw new ItemExportException(ItemExportException.EXPORT_TOO_LARGE,
                        "The overall size of this export is too large.  Please contact your administrator for more information.");
            }
        }

        // if we have any items to process then kick off anonymous thread
        if (itemsMap.size() > 0)
        {
            Thread go = new Thread()
            {
                public void run()
                {
                    Context context = null;
                    ItemIterator iitems = null;
                    try
                    {
                        // create a new dspace context
                        context = new Context();
                        // ignore auths
                        context.turnOffAuthorisationSystem();

                        String fileName = assembleFileName("item", eperson,
                                new Date());
                        String workParentDir = getExportWorkDirectory()
                                + System.getProperty("file.separator")
                                + fileName;
                        String downloadDir = getExportDownloadDirectory(eperson
                                .getID());
                        File dnDir = new File(downloadDir);
                        if (!dnDir.exists() && !dnDir.mkdirs())
                        {
                            log.error("Unable to create download directory");
                        }

                        Iterator<String> iter = itemsMap.keySet().iterator();
                        while(iter.hasNext())
                        {
                            String keyName = iter.next();
                            iitems = new ItemIterator(context, itemsMap.get(keyName));

                            String workDir = workParentDir
                                    + System.getProperty("file.separator")
                                    + keyName;

                            File wkDir = new File(workDir);
                            if (!wkDir.exists() && !wkDir.mkdirs())
                            {
                                log.error("Unable to create working directory");
                            }


                            // export the items using normal export method
                            exportItem(context, iitems, workDir, 1, migrate, false);
                            iitems.close();
                        }

                        // now zip up the export directory created above
                        zip(workParentDir, downloadDir
                                + System.getProperty("file.separator")
                                + fileName + ".zip");
                        // email message letting user know the file is ready for
                        // download
                        emailSuccessMessage(context, eperson, fileName + ".zip");
                        // return to enforcing auths
                        context.restoreAuthSystemState();
                    }
                    catch (Exception e1)
                    {
                        try
                        {
                            emailErrorMessage(eperson, e1.getMessage());
                        }
                        catch (Exception e)
                        {
                            // wont throw here
                        }
                        throw new IllegalStateException(e1);
                    }
                    finally
                    {
                        if (iitems != null)
                        {
                            iitems.close();
                        }

                        // Make sure the database connection gets closed in all conditions.
                        try {
                            context.complete();
                        } catch (SQLException sqle) {
                            context.abort();
                        }
                    }
                }

            };

            go.isDaemon();
            go.start();
        }
        else
        {
            Locale supportedLocale = I18nUtil.getEPersonLocale(eperson);
            emailErrorMessage(eperson, I18nUtil.getMessage("org.dspace.app.itemexport.no-result", supportedLocale));
        }
    }

    /**
     * Create a file name based on the date and eperson
     *
     * @param eperson
     *            - eperson who requested export and will be able to download it
     * @param date
     *            - the date the export process was created
     * @return String representing the file name in the form of
     *         'export_yyy_MMM_dd_count_epersonID'
     * @throws Exception
     */
    public static String assembleFileName(String type, EPerson eperson,
                                          Date date) throws Exception
    {
        // to format the date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MMM_dd");
        String downloadDir = getExportDownloadDirectory(eperson.getID());
        // used to avoid name collision
        int count = 1;
        boolean exists = true;
        String fileName = null;
        while (exists)
        {
            fileName = type + "_export_" + sdf.format(date) + "_" + count + "_"
                    + eperson.getID();
            exists = new File(downloadDir
                    + System.getProperty("file.separator") + fileName + ".zip")
                    .exists();
            count++;
        }
        return fileName;
    }

    /**
     * Use config file entry for org.dspace.app.itemexport.download.dir and id
     * of the eperson to create a download directory name
     *
     * @param ePersonID
     *            - id of the eperson who requested export archive
     * @return String representing a directory in the form of
     *         org.dspace.app.itemexport.download.dir/epersonID
     * @throws Exception
     */
    public static String getExportDownloadDirectory(int ePersonID)
            throws Exception
    {
        String downloadDir = ConfigurationManager
                .getProperty("org.dspace.app.itemexport.download.dir");
        if (downloadDir == null)
        {
            throw new Exception(
                    "A dspace.cfg entry for 'org.dspace.app.itemexport.download.dir' does not exist.");
        }

        return downloadDir + System.getProperty("file.separator") + ePersonID;

    }

    /**
     * Returns config file entry for org.dspace.app.itemexport.work.dir
     *
     * @return String representing config file entry for
     *         org.dspace.app.itemexport.work.dir
     * @throws Exception
     */
    public static String getExportWorkDirectory() throws Exception
    {
        String exportDir = ConfigurationManager
                .getProperty("org.dspace.app.itemexport.work.dir");
        if (exportDir == null)
        {
            throw new Exception(
                    "A dspace.cfg entry for 'org.dspace.app.itemexport.work.dir' does not exist.");
        }
        return exportDir;
    }

    /**
     * Used to read the export archived. Inteded for download.
     *
     * @param fileName
     *            the name of the file to download
     * @param eperson
     *            the eperson requesting the download
     * @return an input stream of the file to be downloaded
     * @throws Exception
     */
    public static InputStream getExportDownloadInputStream(String fileName,
                                                           EPerson eperson) throws Exception
    {
        File file = new File(getExportDownloadDirectory(eperson.getID())
                + System.getProperty("file.separator") + fileName);
        if (file.exists())
        {
            return new FileInputStream(file);
        }
        else
        {
            return null;
        }
    }

    /**
     * Get the file size of the export archive represented by the file name.
     *
     * @param fileName
     *            name of the file to get the size.
     * @throws Exception
     */
    public static long getExportFileSize(String fileName) throws Exception
    {
        String strID = fileName.substring(fileName.lastIndexOf('_') + 1,
                fileName.lastIndexOf('.'));
        File file = new File(
                getExportDownloadDirectory(Integer.parseInt(strID))
                        + System.getProperty("file.separator") + fileName);
        if (!file.exists() || !file.isFile())
        {
            throw new FileNotFoundException("The file "
                    + getExportDownloadDirectory(Integer.parseInt(strID))
                    + System.getProperty("file.separator") + fileName
                    + " does not exist.");
        }

        return file.length();
    }

    public static long getExportFileLastModified(String fileName)
            throws Exception
    {
        String strID = fileName.substring(fileName.lastIndexOf('_') + 1,
                fileName.lastIndexOf('.'));
        File file = new File(
                getExportDownloadDirectory(Integer.parseInt(strID))
                        + System.getProperty("file.separator") + fileName);
        if (!file.exists() || !file.isFile())
        {
            throw new FileNotFoundException("The file "
                    + getExportDownloadDirectory(Integer.parseInt(strID))
                    + System.getProperty("file.separator") + fileName
                    + " does not exist.");
        }

        return file.lastModified();
    }

    /**
     * The file name of the export archive contains the eperson id of the person
     * who created it When requested for download this method can check if the
     * person requesting it is the same one that created it
     *
     * @param context
     *            dspace context
     * @param fileName
     *            the file name to check auths for
     * @return true if it is the same person false otherwise
     */
    public static boolean canDownload(Context context, String fileName)
    {
        EPerson eperson = context.getCurrentUser();
        if (eperson == null)
        {
            return false;
        }
        String strID = fileName.substring(fileName.lastIndexOf('_') + 1,
                fileName.lastIndexOf('.'));
        try
        {
            if (Integer.parseInt(strID) == eperson.getID())
            {
                return true;
            }
        }
        catch (Exception e)
        {
            return false;
        }
        return false;
    }

    /**
     * Reads the download directory for the eperson to see if any export
     * archives are available
     *
     * @param eperson
     * @return a list of file names representing export archives that have been
     *         processed
     * @throws Exception
     */
    public static List<String> getExportsAvailable(EPerson eperson)
            throws Exception
    {
        File downloadDir = new File(getExportDownloadDirectory(eperson.getID()));
        if (!downloadDir.exists() || !downloadDir.isDirectory())
        {
            return null;
        }

        List<String> fileNames = new ArrayList<String>();

        for (String fileName : downloadDir.list())
        {
            if (fileName.contains("export") && fileName.endsWith(".zip"))
            {
                fileNames.add(fileName);
            }
        }

        if (fileNames.size() > 0)
        {
            return fileNames;
        }

        return null;
    }

    /**
     * A clean up method that is ran before a new export archive is created. It
     * uses the config file entry 'org.dspace.app.itemexport.life.span.hours' to
     * determine if the current exports are too old and need pruging
     *
     * @param epersonID
     *            - the id of the eperson to clean up
     * @throws Exception
     */
    public static void deleteOldExportArchives(int epersonID) throws Exception
    {
        int hours = ConfigurationManager
                .getIntProperty("org.dspace.app.itemexport.life.span.hours");
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        now.add(Calendar.HOUR, (-hours));
        File downloadDir = new File(getExportDownloadDirectory(epersonID));
        if (downloadDir.exists())
        {
            File[] files = downloadDir.listFiles();
            for (File file : files)
            {
                if (file.lastModified() < now.getTimeInMillis())
                {
                    if (!file.delete())
                    {
                        log.error("Unable to delete export file");
                    }
                }
            }
        }

    }

    /**
     * A clean up method that is ran before a new export archive is created. It
     * uses the config file entry 'org.dspace.app.itemexport.life.span.hours' to
     * determine if the current exports are too old and need purgeing
     * Removes all old exports, not just those for the person doing the export.
     *
     * @throws Exception
     */
    public static void deleteOldExportArchives() throws Exception
    {
        int hours = ConfigurationManager.getIntProperty("org.dspace.app.itemexport.life.span.hours");
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        now.add(Calendar.HOUR, (-hours));
        File downloadDir = new File(ConfigurationManager.getProperty("org.dspace.app.itemexport.download.dir"));
        if (downloadDir.exists())
        {
            // Get a list of all the sub-directories, potentially one for each ePerson.
            File[] dirs = downloadDir.listFiles();
            for (File dir : dirs)
            {
                // For each sub-directory delete any old files.
                File[] files = dir.listFiles();
                for (File file : files)
                {
                    if (file.lastModified() < now.getTimeInMillis())
                    {
                        if (!file.delete())
                        {
                            log.error("Unable to delete old files");
                        }
                    }
                }

                // If the directory is now empty then we delete it too.
                if (dir.listFiles().length == 0)
                {
                    if (!dir.delete())
                    {
                        log.error("Unable to delete directory");
                    }
                }
            }
        }

    }


    /**
     * Since the archive is created in a new thread we are unable to communicate
     * with calling method about success or failure. We accomplis this
     * communication with email instead. Send a success email once the export
     * archive is complete and ready for download
     *
     * @param context
     *            - the current Context
     * @param eperson
     *            - eperson to send the email to
     * @param fileName
     *            - the file name to be downloaded. It is added to the url in
     *            the email
     * @throws MessagingException
     */
    public static void emailSuccessMessage(Context context, EPerson eperson,
                                           String fileName) throws MessagingException
    {
        try
        {
            Locale supportedLocale = I18nUtil.getEPersonLocale(eperson);
            Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "export_success"));
            email.addRecipient(eperson.getEmail());
            email.addArgument(ConfigurationManager.getProperty("dspace.url") + "/exportdownload/" + fileName);
            email.addArgument(ConfigurationManager.getProperty("org.dspace.app.itemexport.life.span.hours"));

            email.send();
        }
        catch (Exception e)
        {
            log.warn(LogManager.getHeader(context, "emailSuccessMessage", "cannot notify user of export"), e);
        }
    }

    /**
     * Since the archive is created in a new thread we are unable to communicate
     * with calling method about success or failure. We accomplis this
     * communication with email instead. Send an error email if the export
     * archive fails
     *
     * @param eperson
     *            - EPerson to send the error message to
     * @param error
     *            - the error message
     * @throws MessagingException
     */
    public static void emailErrorMessage(EPerson eperson, String error)
            throws MessagingException
    {
        log.warn("An error occurred during item export, the user will be notified. " + error);
        try
        {
            Locale supportedLocale = I18nUtil.getEPersonLocale(eperson);
            Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "export_error"));
            email.addRecipient(eperson.getEmail());
            email.addArgument(error);
            email.addArgument(ConfigurationManager.getProperty("dspace.url") + "/feedback");

            email.send();
        }
        catch (Exception e)
        {
            log.warn("error during item export error notification", e);
        }
    }

    public static void zip(String strSource, String target) throws Exception
    {
        ZipOutputStream cpZipOutputStream = null;
        String tempFileName = target + "_tmp";
        try
        {
            File cpFile = new File(strSource);
            if (!cpFile.isFile() && !cpFile.isDirectory())
            {
                return;
            }
            File targetFile = new File(tempFileName);
            if (!targetFile.createNewFile())
            {
                log.warn("Target file already exists: " + targetFile.getName());
            }

            FileOutputStream fos = new FileOutputStream(tempFileName);
            cpZipOutputStream = new ZipOutputStream(fos);
            cpZipOutputStream.setLevel(9);
            zipFiles(cpFile, strSource, tempFileName, cpZipOutputStream);
            cpZipOutputStream.finish();
            cpZipOutputStream.close();
            cpZipOutputStream = null;

            // Fix issue on Windows with stale file handles open before trying to delete them
            System.gc();

            deleteDirectory(cpFile);
            if (!targetFile.renameTo(new File(target)))
            {
                log.error("Unable to rename file");
            }
        }
        finally
        {
            if (cpZipOutputStream != null)
            {
                cpZipOutputStream.close();
            }
        }
    }

    private static void zipFiles(File cpFile, String strSource,
                                 String strTarget, ZipOutputStream cpZipOutputStream)
            throws Exception
    {
        int byteCount;
        final int DATA_BLOCK_SIZE = 2048;
        FileInputStream cpFileInputStream = null;
        if (cpFile.isDirectory())
        {
            File[] fList = cpFile.listFiles();
            for (int i = 0; i < fList.length; i++)
            {
                zipFiles(fList[i], strSource, strTarget, cpZipOutputStream);
            }
        }
        else
        {
            try
            {
                if (cpFile.getAbsolutePath().equalsIgnoreCase(strTarget))
                {
                    return;
                }
                String strAbsPath = cpFile.getPath();
                String strZipEntryName = strAbsPath.substring(strSource
                        .length() + 1, strAbsPath.length());

                // byte[] b = new byte[ (int)(cpFile.length()) ];

                cpFileInputStream = new FileInputStream(cpFile);

                ZipEntry cpZipEntry = new ZipEntry(strZipEntryName);
                cpZipOutputStream.putNextEntry(cpZipEntry);

                byte[] b = new byte[DATA_BLOCK_SIZE];
                while ((byteCount = cpFileInputStream.read(b, 0,
                        DATA_BLOCK_SIZE)) != -1)
                {
                    cpZipOutputStream.write(b, 0, byteCount);
                }

                // cpZipOutputStream.write(b, 0, (int)cpFile.length());
            }
            finally
            {
                if (cpFileInputStream != null)
                {
                    cpFileInputStream.close();
                }
                cpZipOutputStream.closeEntry();
            }
        }
    }

    private static boolean deleteDirectory(File path)
    {
        if (path.exists())
        {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                if (files[i].isDirectory())
                {
                    deleteDirectory(files[i]);
                }
                else
                {
                    if (!files[i].delete())
                    {
                        log.error("Unable to delete file: " + files[i].getName());
                    }
                }
            }
        }

        boolean pathDeleted = path.delete();
        return (pathDeleted);
    }

}
