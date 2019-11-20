package com.atmire.scripts.deletenonoriginalbundles;

import com.atmire.scripts.ContextScript;
import com.atmire.scripts.Script;
import java.util.Date;
import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;

public class DeleteNonOriginalBundles extends ContextScript {
  private static Logger log = Logger.getLogger(DeleteNonOriginalBundles.class);

  private static int commit_size = 5000;

  public DeleteNonOriginalBundles() {
  }

  public static void main(String[] args) {
    new DeleteNonOriginalBundles().mainImpl(args);
  }

  @Override
  public void run() throws Exception {
    print("Started at " + new Date());
    try {
      iterateOverAllCommunities();
      context.complete();
    } catch (Exception e) {
      printAndLogError(e);
    }
    print("Ended at " + new Date());
  }

  private void iterateOverAllCommunities() throws Exception {
    Community[] communities = Community.findAll(context);
    for (Community community : communities) {
      iterateOverAllCollections(community);
    }
  }

  private void iterateOverAllCollections(Community community) throws Exception {
    print("Treating Community ["+community.getName()+"]");
    Collection[] collections = community.getCollections();
    for (Collection collection : collections) {
      iterateOverAllItemsOfCollection(collection);
      context.commit();
    }
  }

  private void iterateOverAllItemsOfCollection(Collection collection) throws Exception {
    int i = 0;
    print("Treating Collection ["+collection.getName()+"]");
    ItemIterator allItems = collection.getAllItems();
    while (allItems.hasNext()) {
      Item next = allItems.next();
      if (log.isDebugEnabled()) {
        log.debug("Treate item " + next.getID());
      }
      removeNonOriginalBundles(next);
      i++;
      if (i % commit_size == 0 ){
        context.commit();
      }
    }
  }

  private void removeNonOriginalBundles(Item item) throws Exception{
    Bundle[] bundles = item.getBundles();
    for (Bundle bundle : bundles) {
      deleteBundleIfNotOriginal(item, bundle);
    }
  }

  private void deleteBundleIfNotOriginal(Item item, Bundle bundle) throws Exception{
    if (!bundle.getName().equalsIgnoreCase("original")) {
      deletBundle(item, bundle);
    }
  }

  private void deletBundle(Item item, Bundle bundle) throws Exception{
    deletAllBitstreamOfBundle(bundle);
    item.removeBundle(bundle);

  }

  private void deletAllBitstreamOfBundle(Bundle bundle) throws Exception{
    Bitstream[] bitstreams = bundle.getBitstreams();
    for (Bitstream bitstream : bitstreams) {
      bundle.removeBitstream(bitstream);
    }
  }




}
