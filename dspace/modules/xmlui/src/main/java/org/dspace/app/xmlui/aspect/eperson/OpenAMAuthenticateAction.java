package org.dspace.app.xmlui.aspect.eperson;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.sitemap.PatternException;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class OpenAMAuthenticateAction extends AbstractAction {

	/**
	 * Attempt to authenticate the user.
	 */
	public Map act(Redirector redirector, SourceResolver resolver,	Map objectModel, String source, Parameters parameters)
			throws Exception {
		// First check if we are preforming a new login
		Request request = ObjectModelHelper.getRequest(objectModel);

		try {
			Context context = AuthenticationUtil.authenticate(objectModel, null, null, null);

			EPerson eperson = context.getCurrentUser();

			if (eperson != null) {
				// The user has successfully logged in
				String redirectURL = request.getContextPath();

				if (AuthenticationUtil.isInterupptedRequest(objectModel)) {
					// Resume the request and set the redirect target URL to
					// that of the originaly interrupted request.
					redirectURL += AuthenticationUtil.resumeInterruptedRequest(objectModel);
				} 

				// Authentication successfull send a redirect.
				final HttpServletResponse httpResponse = (HttpServletResponse) objectModel
						.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);

				httpResponse.sendRedirect(redirectURL);

				// log the user out for the rest of this current request,
				// however they will be reauthenticated
				// fully when they come back from the redirect. This prevents
				// caching problems where part of the
				// request is preformed fore the user was authenticated and the
				// other half after it succedded. This
				// way the user is fully authenticated from the start of the
				// request.
				context.setCurrentUser(null);

				return new HashMap();
			}
		} catch (Exception ex) {
			throw new PatternException("Unable to preform authentication", ex);
		}

		return null;
	}

}
