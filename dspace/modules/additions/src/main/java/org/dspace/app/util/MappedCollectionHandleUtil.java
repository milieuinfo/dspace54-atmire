package org.dspace.app.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

/**
 * Created by jonas - jonas@atmire.com on 27/06/2018.
 */
public class MappedCollectionHandleUtil {

    /* Log4j logger*/
    private static final Logger log =  Logger.getLogger(MappedCollectionHandleUtil.class);

    public String getIdToUse(String id) {
        if(!id.equals("default") && ! id.matches("[0-9]*/[0-9]*")){
            String configuredHandleProperty = ConfigurationManager.getProperty(id);
            if(StringUtils.isBlank(configuredHandleProperty)){
                log.error("A non existing property has been found during the retrieval of configured handles in the input forms: "+ id +". Please verify this configuration and correct accordingly");
            }else{
                return configuredHandleProperty;
            }
        }
        return id;
    }
}
