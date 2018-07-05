package com.atmire.utils;

import com.atmire.utils.helper.MetadataFieldString;
import com.atmire.utils.subclasses.MetadatumExtended;
import org.dspace.content.*;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 02 Oct 2015
 */
public class ItemUtils {

    public static Set<Bitstream> getBitstreams(Item item) throws SQLException {
        Set<Bitstream> bitstreams = new HashSet<Bitstream>();
        for (Bundle bundle : item.getBundles()) {
            Collections.addAll(bitstreams, bundle.getBitstreams());
        }
        return bitstreams;
    }

    public static Item getItem(DSpaceObject bitstream) {
        if (bitstream == null) {
            return null;
        } else {
            try {
                DSpaceObject parentObject = bitstream.getParentObject();
                if (parentObject instanceof Item) {
                    return (Item) parentObject;
                } else {
                    return getItem(parentObject);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String getMetadataFirstValue(Item item, String fieldName) {
        MetadatumExtended elements
                = MetadataFieldString.encapsulate(fieldName); // this is better not with wildcards
        return getMetadataFirstValue(
                item,
                elements.getSchema(),
                elements.getElement(),
                elements.getQualifier(),
                elements.getLanguage()
        );
    }

    public static String getMetadataFirstValue(
            Item item,
            String schema,
            String element,
            String qualifier,
            String language
    ) {
        Metadatum[] metadata = item.getMetadata(schema, element, qualifier, language);
        String value = null;
        if (metadata.length > 0) {
            value = metadata[0].value;
        }
        return value;
    }
}
