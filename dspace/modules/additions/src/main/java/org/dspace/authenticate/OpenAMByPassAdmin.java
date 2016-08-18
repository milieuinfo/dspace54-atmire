package org.dspace.authenticate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

/**
 * Bypass openam authentication (put it as first in the authentication chain and
 * you will login automatically as admin)
 * 
 * @author lds
 *
 */
public class OpenAMByPassAdmin extends OpenAMImplicitAuthentication {

	@Override
	public int authenticate(Context context, String username, String password,
			String realm, HttpServletRequest request) throws SQLException {

		final String email = "admin@milieuinfo.be";
		final String sn = "Admin";
		final String givenName ="Admin";
		final Collection<String> roles = new ArrayList<String>();
		roles.add("DSpaceAdmin");
	
			try {
				loadGroups(context, roles, request, email);
				final EPerson knownEPerson = EPerson
						.findByEmail(context, email);
				if (knownEPerson == null) {
					// TEMPORARILY turn off authorisation
					
					context.turnOffAuthorisationSystem();
					final EPerson eperson = createEPerson(context, request,email, sn, givenName);
					eperson.update();
					context.commit();
					
					context.restoreAuthSystemState();
					context.setCurrentUser(eperson);

					return SUCCESS;
				} else {
					context.setCurrentUser(knownEPerson);
					return SUCCESS;
				}
			} catch (AuthorizeException e) {
			
				return BAD_ARGS;
			}
		

	}

	@Override
	protected String retrieveOpenAMToken(HttpServletRequest request) {
		return null;
	}

}
