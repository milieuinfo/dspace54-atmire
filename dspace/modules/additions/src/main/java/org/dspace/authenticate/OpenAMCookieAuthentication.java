/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import be.milieuinfo.security.openam.common.OpenAMTools;

import javax.servlet.http.HttpServletRequest;

public class OpenAMCookieAuthentication extends OpenAMImplicitAuthentication {

	@Override
	protected String retrieveOpenAMToken(HttpServletRequest request) {
		if (null == request) {
			return null;
		} else {
			return OpenAMTools.getSSOTokenFromCookies(request.getCookies());
		}
	}
}
