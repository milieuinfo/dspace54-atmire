package com.atmire.authenticate;

import be.milieuinfo.security.openam.api.*;
import org.dspace.core.*;
import org.dspace.eperson.*;

/**
 * @author philip at atmire.com
 */
public interface OpenAMEpersonMetadataMapper {

    void mapToMetadata(Context context, EPerson eperson, OpenAMUserdetails userdetails);
}
