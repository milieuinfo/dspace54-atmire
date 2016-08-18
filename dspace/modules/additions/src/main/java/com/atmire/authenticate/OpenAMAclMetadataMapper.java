package com.atmire.authenticate;

import be.milieuinfo.security.openam.api.*;
import com.atmire.eperson.acl.service.*;
import org.apache.commons.lang.*;
import org.apache.log4j.*;
import org.dspace.core.*;
import org.dspace.eperson.*;

/**
 * @author philip at atmire.com
 */
public class OpenAMAclMetadataMapper implements OpenAMEpersonMetadataMapper {

    private static Logger log = Logger.getLogger(OpenAMAclMetadataMapper.class);

    private String epersonAclMetadataQualifier;
    private String openAMAttribute;
    private EPersonAclMetadataService ePersonAclMetadataService;

    @Override
    public void mapToMetadata(Context context, EPerson eperson, OpenAMUserdetails userdetails) {
        String attributeValue = userdetails.getAttributeValue(openAMAttribute);

        if(StringUtils.isNotBlank(attributeValue)) {
            try {
                ePersonAclMetadataService.updateField(context,eperson, epersonAclMetadataQualifier, attributeValue);
            } catch (Exception e) {
                log.error(e.getMessage(),e);
            }
        }
    }

    public void setEpersonAclMetadataQualifier(String epersonAclMetadataQualifier) {
        this.epersonAclMetadataQualifier = epersonAclMetadataQualifier;
    }

    public void setOpenAMAttribute(String openAMAttribute) {
        this.openAMAttribute = openAMAttribute;
    }

    public void setePersonAclMetadataService(EPersonAclMetadataService ePersonAclMetadataService) {
        this.ePersonAclMetadataService = ePersonAclMetadataService;
    }
}
