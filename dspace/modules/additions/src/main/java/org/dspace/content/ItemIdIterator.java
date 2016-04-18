package org.dspace.content;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;

import java.sql.SQLException;
import java.util.Iterator;

/**
 * Created by Roeland Dillen (roeland at atmire dot com)
 * Date: 08/11/12
 * Time: 17:10
 */
public class ItemIdIterator implements Iterator<Integer> {


    /*
    * This class basically wraps a TableRowIterator.
    */

    /** Our context */
    private Context ourContext;

    /** The table row iterator of Item rows */
    private TableRowIterator itemRows;
    private Long total;
    private Long pos;

    public Long getTotal() {
        return total;
    }

    public Long getPos() {
        return pos;
    }

    /**
     * Construct an item iterator. This is not a public method, since this
     * iterator is only created by CM API methods.
     *
     * @param context
     *            our context
     * @param rows
     *            the rows that correspond to the Items to be iterated over
     */
    ItemIdIterator(Context context, TableRowIterator rows, Long total)
    {
        ourContext = context;
        itemRows = rows;

        this.total=total;
        pos=0L;
    }

    @Override
    public boolean hasNext() {
        try {
            return itemRows.hasNext();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public Integer next() {
        try {
            pos++;
            return itemRows.next().getIntColumn("item_id");
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public void remove() {

        throw new UnsupportedOperationException("Not implemented");
    }


    /**
     * Dispose of this Iterator, and it's underlying resources
     */
    public void close()
    {
        if (itemRows != null)
        {
            itemRows.close();
        }
    }



    /**
     * Get all "final" items in the archive, both archived ("in archive" flag) or
     * withdrawn items are included. The order of the list is indeterminate.
     *
     * @param context
     *            DSpace context object
     * @return an iterator over the items in the archive.
     * @throws SQLException
     */
    public static ItemIdIterator findAllUnfilteredItemIds(Context context) throws SQLException
    {
        String myQuery = "SELECT item_id FROM item WHERE in_archive='1' or withdrawn='1'";
        String totalQuery =  "SELECT count(item_id) as ct FROM item WHERE in_archive='1' or withdrawn='1'";

        TableRowIterator rows = DatabaseManager.queryTable(context, "item", myQuery);
        Long count= DatabaseManager.querySingle(context,totalQuery).getLongColumn("ct");
        return new ItemIdIterator(context, rows,count);
    }
}