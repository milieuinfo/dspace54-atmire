package org.dspace.springmvc;

import be.milieuinfo.security.openam.api.OpenAMIdentityService;
import be.milieuinfo.security.openam.oauth.JerseyBasedOAuthIdentityService;
import be.milieuinfo.security.openam.oauth.JerseyBasedOAuthTokenProvider;
import be.milieuinfo.security.openam.oauth.OAuthLoginUrlProvider;
import be.milieuinfo.security.openam.oauth.OAuthTokenPair;
import be.milieuinfo.security.openam.oauth.OAuthTokenProvider;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.dspace.core.ConfigurationManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OpenAMController {

  public static final String COOKIE_NAME = "iPlanetDirectoryPro";
  public static final String AUTHENTICATION_OPENAM = "authentication-openam";
  public static final String OPENAM_CONSUMER_TOKEN = "openam.consumer.token";
  public static final String OPENAM_CONSUMER_SECRET = "openam.consumer.secret";
  public static final String OPENAM_SERVER_URL = "openam.server.url";
  public static final String OPENAM_PUBLIC_URL = "openam.public.url";
  public static final String ENTRY = "/openamentry";
  public static final String EXIT = "/openamexit";
  public static final String REDIRECT_URL = "/xmlui/openam-login";
  public static final String OAUTH_TOKEN = "oauth_token";
  public static final String OAUTH_VERIFIER = "oauth_verifier";
  public static final String OPENAM_TOKEN = "openam.token";
  public static final String OPENAM_SECRET = "openam.secret";

  private final OAuthTokenPair systemUserTokenPair;
  private final OAuthTokenProvider oAuthTokenProvider;
  private final OpenAMIdentityService identityService;
  private final OAuthLoginUrlProvider oAuthLoginUrlProvider;


  public OpenAMController() {
    String openamPublicUrl = ConfigurationManager
        .getProperty(AUTHENTICATION_OPENAM, OPENAM_PUBLIC_URL);

    String openamInternalUrl = ConfigurationManager
        .getProperty(AUTHENTICATION_OPENAM, OPENAM_SERVER_URL);

    String consumerToken = ConfigurationManager
        .getProperty(AUTHENTICATION_OPENAM, OPENAM_CONSUMER_TOKEN);

    String consumerSecret = ConfigurationManager
        .getProperty(AUTHENTICATION_OPENAM, OPENAM_CONSUMER_SECRET);

    systemUserTokenPair = new OAuthTokenPair(consumerToken, consumerSecret);

    oAuthTokenProvider = new JerseyBasedOAuthTokenProvider();
    ((JerseyBasedOAuthTokenProvider) oAuthTokenProvider).setConsumerToken(systemUserTokenPair);
    ((JerseyBasedOAuthTokenProvider) oAuthTokenProvider).setServerUrl(openamInternalUrl);

    oAuthLoginUrlProvider = new OAuthLoginUrlProvider();
    oAuthLoginUrlProvider.setTokenProvider(oAuthTokenProvider);
    oAuthLoginUrlProvider.setOpenamServerUrl(openamPublicUrl);
    oAuthLoginUrlProvider.setPostLoginUri(ENTRY);

    identityService = new JerseyBasedOAuthIdentityService();
    ((JerseyBasedOAuthIdentityService) identityService).setConsumerToken(systemUserTokenPair);
    ((JerseyBasedOAuthIdentityService) identityService).setServerUrl(openamInternalUrl);

  }


  /**
   * Request token en authorize token
   *
   * @param request
   * @param response
   * @throws Exception
   */
  @RequestMapping(value = EXIT, method = RequestMethod.GET)
  public void doOpenAMStart(HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    String loginUrl = oAuthLoginUrlProvider.getLoginUrl(request);

    response.sendRedirect(loginUrl);
  }

  /**
   * Request an access token and get an SSO-token and store it as a cookie. And redirect to implicit
   * authentication.
   *
   * @param oauthToken
   * @param oauthVerifier
   * @param request
   * @param response
   * @throws Exception
   */
  @RequestMapping(value = ENTRY, method = RequestMethod.GET)
  public void doOpenAMAfterParty(@RequestParam(OAUTH_TOKEN) String oauthToken,
      @RequestParam(OAUTH_VERIFIER) String oauthVerifier, HttpServletRequest request,
      HttpServletResponse response) throws Exception {

    OAuthTokenPair accessToken = oAuthTokenProvider
        .getAccessToken(getAuthorizedTokenFromSession(request), oauthVerifier);

    String ssoToken = identityService.getSsoToken(accessToken.getToken(), accessToken.getSecret());

    Cookie cookie = new Cookie(COOKIE_NAME, ssoToken);

    response.addCookie(cookie);

    response.sendRedirect(REDIRECT_URL);
  }

  private OAuthTokenPair getAuthorizedTokenFromSession(HttpServletRequest request) {
    HttpSession session = request.getSession();
    String openamToken = (String) session.getAttribute(OPENAM_TOKEN);
    String openamSecret = (String) session.getAttribute(OPENAM_SECRET);
    return new OAuthTokenPair(openamToken, openamSecret);
  }

}
