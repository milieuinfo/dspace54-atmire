package com.atmire.clamav;

import com.atmire.consumer.AsynchronousConsumer;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.ctask.general.ClamScan;

/**
 * @author philip at atmire.com
 */
public class AsynchronousVirusScanConsumer implements AsynchronousConsumer {

    private static Logger log = Logger.getLogger(AsynchronousVirusScanConsumer.class);

    private int objectId;

    private int objectType;

    @Override
    public void run() {
        Context context = null;

        try {
            if (objectId > 0 && objectType > 0) {
                context = new Context();
                context.turnOffAuthorisationSystem();

                switch (objectType) {
                    case Constants.ITEM:
                        Item item = Item.find(context, objectId);

                        if (item != null && ArrayUtils.isNotEmpty(item.getNonInternalBitstreams())) {
                            ClamScan clamScan = new ClamScan();
                            clamScan.init(null, null);
                            clamScan.perform(item);
                        }
                        break;
                }
                context.complete();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (context != null && context.isValid()) {
                context.abort();
            }
        }
    }

    @Override
    public void setObjectId(int objectId) {
        this.objectId = objectId;
    }

    @Override
    public void setObjectType(int objectType) {
        this.objectType = objectType;
    }
}
