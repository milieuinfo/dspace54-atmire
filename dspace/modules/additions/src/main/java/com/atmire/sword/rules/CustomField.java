package com.atmire.sword.rules;

import java.sql.*;
import java.util.*;
import org.apache.commons.lang3.*;
import org.dspace.content.*;
import org.dspace.core.*;

public enum CustomField {

    BITSTREAM_COUNT("bitstream.count") {
        public List<Metadatum> createValueList(final Context context, final Item item) throws SQLException {
            List<Metadatum> output = new LinkedList<Metadatum>();
            for (Bundle bundle : item.getBundles()) {
                for (Bitstream bitstream : bundle.getBitstreams()) {
                    Metadatum value = new Metadatum();
                    value.value = bitstream.getName();
                    output.add(value);
                }
            }

            return output;
        }
    },
    ITEM_LIFECYCLE_STATUS("item.status") {
        public List<Metadatum> createValueList(final Context context, final Item item) throws SQLException {
            List<Metadatum> output = new LinkedList<Metadatum>();

            Metadatum value = new Metadatum();
            if(item.isArchived()) {
                value.value = "archived";
            } else if(item.isWithdrawn()) {
                value.value = "withdrawn";
            } else if(WorkspaceItem.findByItem(context, item) != null) {
                value.value = "workspace";
            } else {
                value.value = "workflow";
            }

            output.add(value);
            return output;
        }
    };

    private final String fieldName;

    CustomField(final String field) {
        this.fieldName = field;
    }

    public String getFieldName() {
        return fieldName;
    }

    public abstract List<Metadatum> createValueList(final Context context, final Item item) throws SQLException;

    public static CustomField findByField(final String field) {
        CustomField result = null;

        for (CustomField customField : CustomField.values()) {
            if(StringUtils.equals(customField.getFieldName(), field)) {
                result = customField;
                break;
            }
        }

        return result;
    }

}
