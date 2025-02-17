package com.atmire.lne.swordv2;

import java.util.Map;

import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.sword2.AbstractSimpleDC;
import org.dspace.sword2.DSpaceSwordException;
import org.dspace.sword2.SimpleDCMetadata;
import org.dspace.sword2.SwordEntryDisseminator;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

public class LneDCEntryDisseminator extends AbstractSimpleDC implements SwordEntryDisseminator {
    public LneDCEntryDisseminator() {
    }

    public DepositReceipt disseminate(Context context, Item item, DepositReceipt receipt)
        throws DSpaceSwordException, SwordError, SwordServerException
    {
        SimpleDCMetadata md = this.getMetadata(item);

        Map<String, String> dc = md.getDublinCore();
        for (String element : dc.keySet())
        {
            String value = dc.get(element);
            receipt.addDublinCore(element, value);
        }

        Map<String, String> atom = md.getAtom();
        for (String element : atom.keySet())
        {
            String value = atom.get(element);
            if ("author".equals(element))
            {
                receipt.getWrappedEntry().addAuthor(value);
            }
            else if ("published".equals(element))
            {
                receipt.getWrappedEntry().setPublished(getDateFormatted(value));
            }
            else if ("rights".equals(element))
            {
                receipt.getWrappedEntry().setRights(value);
            }
            else if ("summary".equals(element))
            {
                receipt.getWrappedEntry().setSummary(value);
            }
            else if ("title".equals(element))
            {
                receipt.getWrappedEntry().setTitle(value);
            }
            else if ("updated".equals(element))
            {
                receipt.getWrappedEntry().setUpdated(getDateFormatted(value));
            }
        }

        return receipt;
    }

    private String getDateFormatted(String value) {
        return new DCDate(new DCDate(value).toDate()).toString();
    }
}
