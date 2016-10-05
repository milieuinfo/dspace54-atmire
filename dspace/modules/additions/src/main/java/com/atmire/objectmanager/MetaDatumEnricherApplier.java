package com.atmire.objectmanager;

import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by jonas - jonas@atmire.com on 05/10/16.
 */
public class MetaDatumEnricherApplier {

    @Autowired
    private List<MetaDatumEnricher> metaDatumEnrichers;

    public Metadatum[] enrichMetaData(final Metadatum[] dcvs, Context context) {
        List<Metadatum> enrichedList = new ArrayList<>(Arrays.asList(dcvs));

        for (MetaDatumEnricher metaDatumEnricher : metaDatumEnrichers) {
            metaDatumEnricher.enrichMetadata(context, enrichedList);
        }

        return enrichedList.toArray(dcvs);
    }

    public Metadatum[] enrichMetaData(Item item, Context context) {
        Metadatum[] dcvs = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        return enrichMetaData(dcvs, context);
    }


}
