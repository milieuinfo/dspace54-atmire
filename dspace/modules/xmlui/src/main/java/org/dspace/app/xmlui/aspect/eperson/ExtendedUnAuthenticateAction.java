package org.dspace.app.xmlui.aspect.eperson;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authenticate.OpenAMAuthentication;
import org.dspace.core.Context;

public class ExtendedUnAuthenticateAction extends UnAuthenticateAction{

  @Override
  public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source,
      Parameters parameters) throws Exception {
    // Remove OpenAM attributes from session if they are present...

    Context context = ContextUtil.obtainContext(objectModel);
    final HttpServletRequest httpRequest =
        (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);

    HttpSession session = httpRequest.getSession();

    session.removeAttribute(OpenAMAuthentication.SPECIAL_GROUP_REQUEST_ATTRIBUTE);

    return super.act(redirector, resolver, objectModel, source, parameters);
  }
}
