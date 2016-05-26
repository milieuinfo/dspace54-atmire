package com.atmire.lne.swordv2;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.dspace.app.itemimport.ItemImport;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.sword2.AbstractSwordContentIngester;
import org.dspace.sword2.DSpaceSwordException;
import org.dspace.sword2.DepositResult;
import org.dspace.sword2.VerboseDescription;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public class SwordSAFSIPIngester extends AbstractSwordContentIngester {

    private static Logger log = Logger.getLogger(SwordSAFSIPIngester.class);

    @Override
    public DepositResult ingestToCollection(Context context, Deposit deposit, Collection collection, VerboseDescription verboseDescription, DepositResult result)
            throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException {

        File mapFile = null;
        File unzippedPackage = null;
        File pkgFile = deposit.getFile();

        try {
            mapFile = createTemporaryMapFile(pkgFile.getName());

            log.info("Attempting Sword DSpace SAF SIP deposit to collection: " + collection.getName()
                    + ", zip: " + pkgFile.getName()
                    + ", map: " + mapFile.getAbsolutePath());

            unzippedPackage = unzipPackageFile(pkgFile);

            int numberOfItemsInZip = unzippedPackage.listFiles().length;
            if (numberOfItemsInZip <= 0) {
                throw new DSpaceSwordException("Cannot import an empty package");

            } else if (numberOfItemsInZip > 1) {
                throw new DSpaceSwordException("A package should contain at most one item");

            } else {
                ItemImport itemImport = new ItemImport();
                List<Item> importedItems = itemImport.addItemsAtomic(context, new Collection[]{collection}, unzippedPackage.getAbsolutePath(), mapFile.getAbsolutePath(), true);

                if (CollectionUtils.isEmpty(importedItems)) {
                    return null;
                } else {
                    DepositResult depositResult = new DepositResult();
                    depositResult.setItem(importedItems.get(0));
                    depositResult.setTreatment(this.getTreatment());
                    depositResult.setDerivedResources(Arrays.asList(importedItems.get(0).getNonInternalBitstreams()));

                    return depositResult;
                }
            }
        } catch (Exception e) {
            String message = "Failure during item import: " + e.getMessage();
            log.error(message);
            throw new DSpaceSwordException(message, e);

        } finally {
            if(mapFile != null) {
                mapFile.delete();
            }
            if(unzippedPackage != null) {
                try {
                    FileUtils.deleteDirectory(unzippedPackage);
                } catch (IOException e) {
                    log.warn("Unable to delete temp directory " + unzippedPackage.getAbsolutePath());
                }
            }
        }
    }

    private File unzipPackageFile(final File pkgFile) throws DSpaceSwordException {
        try {
            String sourceDir = ItemImport.unzip(pkgFile, ItemImport.getTempWorkDir());
            return new File(sourceDir);
        } catch (IOException e) {
            String message = "Unable to unzip file " + pkgFile.getName();
            log.error(message, e);
            throw new DSpaceSwordException(message, e);
        }
    }

    private File createTemporaryMapFile(final String pkgFileName) throws DSpaceSwordException {
        File mapFile = null;
        try {
            mapFile = File.createTempFile(pkgFileName, ".map", ItemImport.getTempWorkDirFile());
        } catch (IOException e) {
            String message = "Unable to create temporary mapfile";
            log.error(message, e);
            throw new DSpaceSwordException(message, e);
        }
        return mapFile;
    }

    /**
     * The human readable description of the treatment this ingester has
     * put the deposit through
     *
     * @return
     * @throws DSpaceSwordException
     */
    private String getTreatment() throws DSpaceSwordException {
        return "The package has been ingested and unpacked into an item.  Template metadata for " +
                "the collection has been used and the item has been enriched with the provided metadata.";
    }

    @Override
    public DepositResult ingestToItem(Context context, Deposit deposit, Item item, VerboseDescription verboseDescription, DepositResult result)
            throws DSpaceSwordException, SwordError, SwordAuthException {
        return null;
    }

}
