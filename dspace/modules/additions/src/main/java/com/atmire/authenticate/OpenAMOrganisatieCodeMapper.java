package com.atmire.authenticate;

import be.milieuinfo.core.domain.OrganisatieCode;
import be.milieuinfo.security.openam.api.OpenAMUserdetails;
import com.atmire.eperson.acl.service.EPersonAclMetadataService;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * @author philip at atmire.com
 */
public class OpenAMOrganisatieCodeMapper implements OpenAMEpersonMetadataMapper {

    private static Logger log = Logger.getLogger(OpenAMOrganisatieCodeMapper.class);

    private String epersonAclMetadataQualifier;
    private EPersonAclMetadataService ePersonAclMetadataService;

    @Override
    public void mapToMetadata(Context context, EPerson eperson, OpenAMUserdetails userdetails) {
        OrganisatieCode organisatieCode = userdetails.getOrganisatieCode();

        if(eperson != null && context != null) {
            if (organisatieCode != null) {

                log.info(String.format("User %s has following value for his organisation code (mapped to %s): %s", eperson.getEmail(),
                        epersonAclMetadataQualifier, organisatieCode.toString()));

                try {
                    ePersonAclMetadataService.updateField(context, eperson, epersonAclMetadataQualifier, organisatieCode.toString());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                log.warn("No organisation code received for user " + eperson.getEmail());
            }
        }
    }

    public void setEpersonAclMetadataQualifier(String epersonAclMetadataQualifier) {
        this.epersonAclMetadataQualifier = epersonAclMetadataQualifier;
    }
    
    public void setePersonAclMetadataService(EPersonAclMetadataService ePersonAclMetadataService) {
        this.ePersonAclMetadataService = ePersonAclMetadataService;
    }
}
