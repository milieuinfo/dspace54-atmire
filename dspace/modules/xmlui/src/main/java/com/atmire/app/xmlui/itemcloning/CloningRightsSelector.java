package com.atmire.app.xmlui.itemcloning;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.selection.Selector;

import java.util.Map;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 19 Jun 2018
 */
public class CloningRightsSelector implements Selector {

    @Override
    public boolean select(String expression, Map objectModel, Parameters parameters) {
        return CloneItemUtils.authorizeCloning(objectModel);
    }
}
