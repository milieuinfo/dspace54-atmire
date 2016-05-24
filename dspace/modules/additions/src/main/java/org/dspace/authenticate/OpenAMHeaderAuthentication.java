/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import be.milieuinfo.security.openam.api.OpenAMTokenManager;

import javax.servlet.http.HttpServletRequest;

public class OpenAMHeaderAuthentication extends OpenAMImplicitAuthentication {

	@Override
	protected String retrieveOpenAMToken(HttpServletRequest request) {
		if (null == request) {
			return null;
		} else {
			return request.getHeader(OpenAMTokenManager.OPENAM_SSO_ID_HEADER);
		}
	}

}
