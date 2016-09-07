package org.dspace.content;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 05 Jul 2016
 */
public class BitstreamQuerier {

    private static final BitstreamQuerier instance = new BitstreamQuerier();

    public static BitstreamQuerier getInstance() {
        return instance;
    }

    public List<Integer> findByVirusScanDate(Context context, int queryLimit) {
        try {
            List<Integer> ids = new LinkedList<>();

            String query = "SELECT bitstream_id\n"
                    + "FROM bitstream LEFT JOIN \n"
                    + "    (SELECT text_value, metadatavalue.resource_id\n"
                    + "    FROM metadatafieldregistry, metadataschemaregistry, metadatavalue\n"
                    + "    WHERE metadatavalue.metadata_field_id = metadatafieldregistry.metadata_field_id\n"
                    + "    AND metadatafieldregistry.metadata_schema_id = metadataschemaregistry.metadata_schema_id\n"
                    + "    AND resource_type_id = 0\n"
                    + "    AND metadataschemaregistry.short_id = 'bitstream'\n"
                    + "    AND metadatafieldregistry.qualifier = 'lastScanDate') \n"
                    + "AS m\n"
                    + "ON bitstream_id = resource_id\n"
                    + "ORDER BY CASE WHEN text_value IS NULL THEN '0' ELSE text_value END\n"
                    + "LIMIT ?";
            TableRowIterator tri = DatabaseManager.query(context, query, queryLimit);

            try {
                while (tri.hasNext()) {
                    TableRow row = tri.next();
                    ids.add(row.getIntColumn("bitstream_id"));
                }
            } finally {
                // close the TableRowIterator to free up resources
                tri.close();
            }

            return ids;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
