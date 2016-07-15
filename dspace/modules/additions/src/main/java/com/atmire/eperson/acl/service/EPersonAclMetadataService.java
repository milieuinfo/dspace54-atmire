package com.atmire.eperson.acl.service;

import java.io.*;
import java.sql.*;
import org.dspace.authorize.*;
import org.dspace.content.*;
import org.dspace.core.*;
import org.dspace.eperson.*;

/**
 * @author philip at atmire.com
 */
public interface EPersonAclMetadataService {
    void updateField(Context context, EPerson person, String qualifier, String value) throws SQLException, AuthorizeException, IOException, NonUniqueMetadataException;

    void removeField(Context context,EPerson person, String qualifier) throws SQLException, AuthorizeException;

    boolean fieldContainsSchemaAndElement(String fieldToUpdate);
}
