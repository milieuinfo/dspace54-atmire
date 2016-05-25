package com.atmire.lne.swordv2;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.packager.AbstractPackageIngester;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.Context;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public class SwordDIMSIPIngester extends AbstractPackageIngester {
	/**
	 * Create new DSpaceObject out of the ingested package.  The object
	 * is created under the indicated parent.  This creates a
	 * <code>DSpaceObject</code>.  For Items, it is up to the caller to
	 * decide whether to install it or submit it to normal DSpace Workflow.
	 * <p>
	 * The deposit license (Only significant for Item) is passed
	 * explicitly as a string since there is no place for it in many
	 * package formats.  It is optional and may be given as
	 * <code>null</code>.
	 * <p>
	 * Use <code>ingestAll</code> method to perform a recursive ingest of all
	 * packages which are referenced by an initial package.
	 *
	 * @param context DSpace context.
	 * @param parent  parent under which to create new object
	 *                (may be null -- in which case ingester must determine parent from package
	 *                or throw an error).
	 * @param pkgFile The package file to ingest
	 * @param params  Properties-style list of options (interpreted by each packager).
	 * @param license may be null, which takes default license.
	 * @return DSpaceObject created by ingest.
	 * @throws PackageValidationException if package is unacceptable or there is
	 *                                    a fatal error turning it into a DSpaceObject.
	 */
	@Override
	public DSpaceObject ingest(Context context, DSpaceObject parent, File pkgFile, PackageParameters params, String license) throws PackageException, CrosswalkException, AuthorizeException, SQLException, IOException {
		return null;
	}

	/**
	 * Replace an existing DSpace Object with contents of the ingested package.
	 * The packager <em>may</em> choose not to implement <code>replace</code>,
	 * since it somewhat contradicts the archival nature of DSpace.
	 * The exact function of this method is highly implementation-dependent.
	 * <p>
	 * Use <code>replaceAll</code> method to perform a recursive replace of
	 * objects referenced by a set of packages.
	 *
	 * @param context DSpace context.
	 * @param dso     existing DSpace Object to be replaced, may be null
	 *                if object to replace can be determined from package
	 * @param pkgFile The package file to ingest.
	 * @param params  Properties-style list of options specific to this packager
	 * @return DSpaceObject with contents replaced
	 * @throws PackageValidationException    if package is unacceptable or there is
	 *                                       a fatal error turning it into an Item.
	 * @throws UnsupportedOperationException if this packager does not
	 *                                       implement <code>replace</code>.
	 */
	@Override
	public DSpaceObject replace(Context context, DSpaceObject dso, File pkgFile, PackageParameters params) throws PackageException, UnsupportedOperationException, CrosswalkException, AuthorizeException, SQLException, IOException {
		return null;
	}

	/**
	 * Returns a user help string which should describe the
	 * additional valid command-line options that this packager
	 * implementation will accept when using the <code>-o</code> or
	 * <code>--option</code> flags with the Packager script.
	 *
	 * @return a string describing additional command-line options available
	 * with this packager
	 */
	@Override
	public String getParameterHelp() {
		return null;
	}
}
