package com.atmire.eperson.acl.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import java.io.IOException;
import java.sql.SQLException;

/**
 * @author philip at atmire.com
 */
public class EPersonAclMetadataServiceBean implements EPersonAclMetadataService {

    private String schema;
    private String element;

    @Override
    public void updateField(Context context, EPerson person, String qualifier, String value) throws SQLException, AuthorizeException, IOException, NonUniqueMetadataException {
        context.turnOffAuthorisationSystem();
        createIfNewMetadaField(context, qualifier);
        person.clearMetadata(schema, element, qualifier, Item.ANY);
        person.addMetadata(schema, element, qualifier, null, value);
        person.update();
        context.commit();
        context.restoreAuthSystemState();
    }

    @Override
    public void removeField(Context context, EPerson person, String qualifier) throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        person.clearMetadata(schema, element, qualifier, Item.ANY);
        person.update();
        context.commit();
        context.restoreAuthSystemState();
    }

    @Override
    public void removeAllFields(Context context, EPerson person) throws SQLException, AuthorizeException {
        removeField(context, person, Item.ANY);
    }

    @Override
    public boolean fieldContainsSchemaAndElement(String fieldToUpdate) {
        return fieldToUpdate.contains(schema + "." + element);
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public void setElement(String element) {
        this.element = element;
    }

    private void createIfNewMetadaField(Context context, String qualifier) throws SQLException, NonUniqueMetadataException, IOException, AuthorizeException {
        MetadataSchema metadataSchema = MetadataSchema.find(context, schema);
        MetadataField field = MetadataField.findByElement(context,
                metadataSchema.getSchemaID(), element, qualifier);

        if(field == null){
            field = new MetadataField();
            field.setSchemaID(metadataSchema.getSchemaID());
            field.setElement(element);
            field.setQualifier(qualifier);
            field.create(context);
            context.commit();
        }
    }
}
