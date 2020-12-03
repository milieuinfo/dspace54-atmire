/**
 * The contents of this file are subject to the license and copyright detailed in the LICENSE and
 * NOTICE files at the root of the source tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import org.dspace.administer.MetadataImporter;
import org.dspace.administer.RegistryLoader;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.FlywayCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a FlywayCallback class which automatically updates the Metadata Schema Registry and
 * Bitstream Formats Registries BEFORE any Database migration occurs.
 * <P>
 * The reason this runs BEFORE a migration is to ensure that any new metadata fields are FIRST added
 * to our registries, so that the migrations can make use of those new metadata fields, etc.
 * <P>
 * However, there is one exception. If this is a "fresh install" of DSpace, we'll need to wait until
 * the necessary database tables are created. In that scenario we will load registries AFTER the
 * initial migration.
 *
 * @author Tim Donohue
 */
public class DatabaseRegistryUpdater implements FlywayCallback {

  /**
   * logging category
   */
  private static final Logger log = LoggerFactory.getLogger(DatabaseRegistryUpdater.class);

  // Whether or not this is a fresh install of DSpace
  // This determines whether to update registries PRE or POST migration
  private boolean freshInstall = false;

  /**
   * Method to actually update our registries from latest configs
   */
  private void updateRegistries()
      throws Exception {
    final Context context = new Context();
    try {
      context.turnOffAuthorisationSystem();

      String base = ConfigurationManager.getProperty("dspace.dir")
          + File.separator + "config" + File.separator
          + "registries" + File.separator;

      readConfigAndUpdateRegistry(context, base);

      context.restoreAuthSystemState();
      // Commit changes and close context
      context.complete();
      log.info("All Bitstream Format Regitry and Metadata Registry updates were completed.");
    } finally {
      // Clean up our context, if it still exists & it was never completed
      if (context.isValid()) {
        context.abort();
      }
    }
  }

  private void readConfigAndUpdateRegistry(final Context context, String base) throws IOException {
    Files.walkFileTree(Paths.get(base), new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        String filename = file.getFileName().toString();
        log.info("Found file " + filename + " in registries folder");
        if (filename.endsWith("formats.xml")) {
          updateBitstreamFormats(file.toAbsolutePath().toString(), context);
        } else if (filename.endsWith("workflow-types.xml")) {
          // Check if XML Workflow is enabled in workflow.cfg
          if (ConfigurationManager.getProperty("workflow", "workflow.framework")
              .equals("xmlworkflow")) {
            // If so, load in the workflow metadata types as well
            updateRegistry(file.toAbsolutePath().toString());
          }
        } else if (filename.endsWith("types.xml")) {
          updateRegistry(file.toAbsolutePath().toString());
        }
        return CONTINUE;
      }
    });
  }

  private void updateRegistry(String filename) {
    // Load updates to Metadata schema registries (if any)
    log.info("Updating Metadata Registries based on metadata type configs in " + filename);
    try {
      MetadataImporter.loadRegistry(filename, true);
    } catch (Exception e) {
      log.error("Error attempting to update Registry", e);
    }
  }

  private void updateBitstreamFormats(String filename, Context context) {
    // Load updates to Bitstream format registry (if any)
    log.info("Updating Bitstream Format Registry based on " + filename);
    try {
      RegistryLoader.loadBitstreamFormats(context, filename);
    } catch (Exception e) {
      log.error("Error attempting to update Bitstream Format", e);
    }
  }


  @Override
  public void afterClean(Connection connection) {
    // do nothing
  }

  @Override
  public void afterEachMigrate(Connection connection, MigrationInfo info) {
    // do nothing
  }

  @Override
  public void afterInfo(Connection connection) {
    if (!freshInstall) {
      try {
        updateRegistries();
      } catch (Exception e) {
        log.error("Error attempting to update Bitstream Format and/or Metadata Registries", e);
      }
    }
  }

  @Override
  public void afterInit(Connection connection) {
    // do nothing
  }

  @Override
  public void afterMigrate(Connection connection) {
    // If this is a fresh install, we must update registries AFTER the
    // initial migrations (since the registry tables won't exist until the
    // initial migrations are performed)
    if (freshInstall) {
      try {
        updateRegistries();
      } catch (Exception e) {
        log.error("Error attempting to update Bitstream Format and/or Metadata Registries", e);
      }
      freshInstall = false;
    }

    // After every migrate, ensure default Groups are setup correctly.
    Context context = null;
    try {
      context = new Context();
      context.turnOffAuthorisationSystem();
      // While it's not really a formal "registry", we need to ensure the
      // default, required Groups exist in the DSpace database
      Group.initDefaultGroupNames(context);
      context.restoreAuthSystemState();
      // Commit changes and close context
      context.complete();
    } catch (Exception e) {
      log.error("Error attempting to add/update default DSpace Groups", e);
    } finally {
      // Clean up our context, if it still exists & it was never completed
      if (context != null && context.isValid()) {
        context.abort();
      }
    }
  }

  @Override
  public void afterRepair(Connection connection) {
    // do nothing
  }

  @Override
  public void afterValidate(Connection connection) {
    // do nothing
  }

  @Override
  public void beforeClean(Connection connection) {
    // do nothing
  }

  @Override
  public void beforeEachMigrate(Connection connection, MigrationInfo info) {
    // do nothing
  }

  @Override
  public void beforeInfo(Connection connection) {
    // do nothing
  }

  @Override
  public void beforeInit(Connection connection) {
    // do nothing
  }

  @Override
  public void beforeMigrate(Connection connection) {
    // Check if our MetadataSchemaRegistry table exists yet.
    // If it does NOT, then this is a fresh install & we'll need to
    // updateRegistries() AFTER migration
    if (DatabaseUtils.tableExists(connection, "MetadataSchemaRegistry")) {
      // Ensure registries are updated BEFORE a database migration (upgrade)
      // We need to ensure any new metadata fields are added before running
      // migrations, just in case the migrations need to utilize those new fields
      try {
        updateRegistries();
      } catch (Exception e) {
        log.error("Error attempting to update Bitstream Format and/or Metadata Registries", e);
      }
    } else {
      // this is a fresh install, need to migrate first in order to create
      // the registry tables.
      freshInstall = true;
    }
  }

  @Override
  public void beforeRepair(Connection connection) {
    // do nothing
  }

  @Override
  public void beforeValidate(Connection connection) {
    // do nothing
  }
}
