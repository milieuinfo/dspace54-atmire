package com.atmire.clamav;

import com.atmire.consumer.*;
import org.apache.log4j.*;
import org.dspace.content.*;
import org.dspace.core.*;
import org.dspace.ctask.general.*;

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

                        Bundle bundle = item.getBundles("ORIGINAL")[0];

                        if (bundle != null && bundle.getBitstreams().length > 0) {
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
