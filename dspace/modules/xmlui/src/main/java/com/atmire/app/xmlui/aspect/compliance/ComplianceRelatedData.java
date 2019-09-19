package com.atmire.app.xmlui.aspect.compliance;

import com.atmire.sword.result.ComplianceResult;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 11 Jun 2018
 */
public interface ComplianceRelatedData {
    void renderRelatedData(Context context, Item item, ComplianceResult result, Division div)
            throws WingException;
}
