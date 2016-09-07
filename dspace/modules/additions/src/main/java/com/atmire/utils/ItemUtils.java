package com.atmire.utils;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;

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
}
