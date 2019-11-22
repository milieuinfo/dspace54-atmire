package com.atmire.scripts.deletenonoriginalbundles;

import com.atmire.scripts.ContextScript;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

public class DeleteNonOriginalBundles extends ContextScript {

  private static Logger log = Logger.getLogger(DeleteNonOriginalBundles.class);

  private static int commitSize = 100;

  public DeleteNonOriginalBundles() {
  }

  public static void main(String[] args) {
    new DeleteNonOriginalBundles().mainImpl(args);
  }

  @Override
  public void run() throws Exception {
    print("Started at " + new Date());
    try {
      iterateOverAllItemsThatHaveMoreThanOneBundle();
      context.complete();
    } catch (Exception e) {
      printAndLogError(e);
    }
    print("Ended at " + new Date());
  }


  private void iterateOverAllItemsThatHaveMoreThanOneBundle() throws Exception {
    String query = "select item_id from item2bundle group by item_id having count(*) > 1";
    List<TableRow> rows = DatabaseManager.queryTable(context, "item2bundle", query).toList();
    print(rows.size() + " items to treat");
    int i = 0;
    long begin = System.currentTimeMillis();
    for (TableRow row : rows) {
      int itemId = row.getIntColumn("item_id");

      Item item = Item.find(context, itemId);
      removeNonOriginalBundles(item);
      i++;
      if (i % commitSize == 0) {
        long end = System.currentTimeMillis();
        print("Treated " + i + " items comitting (100 items took "+ (end-begin)+"ms)");
        context.commit();
        begin= System.currentTimeMillis();
      }
    }

  }

  private void removeNonOriginalBundles(Item item) throws Exception {
    Bundle[] bundles = item.getBundles();
    for (Bundle bundle : bundles) {
      deleteBundleIfNotOriginal(item, bundle);
    }
  }

  private void deleteBundleIfNotOriginal(Item item, Bundle bundle) throws Exception {
    if (!bundle.getName().equalsIgnoreCase("original")) {
      deletBundle(item, bundle);
    }
  }

  private void deletBundle(Item item, Bundle bundle) throws Exception {
    deletAllBitstreamOfBundle(bundle);
    item.removeBundle(bundle);
  }

  private void deletAllBitstreamOfBundle(Bundle bundle) throws Exception {
    Bitstream[] bitstreams = bundle.getBitstreams();
    for (Bitstream bitstream : bitstreams) {
      bundle.removeBitstream(bitstream);
    }
  }


}
