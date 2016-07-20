/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import be.milieuinfo.security.openam.api.*;
import be.milieuinfo.security.openam.oauth.*;
import com.sun.jersey.api.client.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import javax.servlet.http.*;
import javax.ws.rs.core.*;
import org.apache.commons.collections.*;
import org.apache.commons.lang.*;
import org.apache.log4j.*;
import org.dspace.authorize.*;
import org.dspace.core.*;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.*;

public abstract class OpenAMAuthentication implements AuthenticationMethod {

    private static Logger log = Logger.getLogger(OpenAMAuthentication.class);

    private static final String ADMINISTRATOR_GROUP = "Administrator";
    private static final String DSPACE_ADMIN_ROLE = "DSpaceAdmin";
    private static final String DSPACE_ROLE_PREFIX = "DSpace";
    public final static String SPECIAL_GROUP_REQUEST_ATTRIBUTE = "openam.specialgroup";

    protected DSpaceJerseyBasedOAuthIdentityService openAMIdentityService;

    protected OpenAMAuthentication() {
        final String openamServerUrl = ConfigurationManager.getProperty("authentication-openam", "openam.server.url");
        final String consumerToken = ConfigurationManager.getProperty("authentication-openam", "openam.consumer.token");
        final String consumerSecret = ConfigurationManager.getProperty("authentication-openam", "openam.consumer.secret");

        this.openAMIdentityService = new DSpaceJerseyBasedOAuthIdentityService();
        this.openAMIdentityService.setUrl(openamServerUrl);
        this.openAMIdentityService.setConsumerToken(new OAuthTokenPair(consumerToken, consumerSecret));
    }

    public int authenticateOpenAM(Context context, HttpServletRequest request, String ssoId) throws SQLException {
        if (!StringUtils.isBlank(ssoId)) {
            final OpenAMUserdetails userDetails = this.openAMIdentityService.getUserDetails(ssoId);
            if (userDetails != null) {
                final String userName = userDetails.getUsername();
            	
            	final String email = userDetails.getAttributeValue("mail") == null ? userName : userDetails.getAttributeValue("mail");
                final String sn = userDetails.getAttributeValue("sn") == null ? userName : userDetails.getAttributeValue("sn");
                final String givenName = userDetails.getAttributeValue("givenName") ==null ? userName : userDetails.getAttributeValue("givenName");
                
                final Collection<String> roles = userDetails.getRoles();
                if (!StringUtils.isBlank(email)) {
                    try {
                        loadGroups(context, roles, request);
                        final EPerson knownEPerson = EPerson.findByEmail(context, email);
                        if (knownEPerson == null) {
                            // TEMPORARILY turn off authorisation
                            context.turnOffAuthorisationSystem();
                            final EPerson eperson = createEPerson(context, request, email, sn, givenName);
                            eperson.update();
                            context.commit();
                            context.restoreAuthSystemState();
                            context.setCurrentUser(eperson);
                            log.info(LogManager.getHeader(context, "login", "type=openam-interactive"));
                            return SUCCESS;
                        } else {
                            context.setCurrentUser(knownEPerson);
                            return SUCCESS;
                        }
                    } catch (AuthorizeException e) {
                        log.warn(LogManager.getHeader(context, "authorize_exception", ""), e);
                        return BAD_ARGS;
                    }
                } else {
                    return BAD_ARGS;
                }
            } else {
                return NO_SUCH_USER;
            }
        } else {
            return NO_SUCH_USER;
        }
    }

    protected EPerson createEPerson(Context context, HttpServletRequest request, String email, String sn, String givenName) throws SQLException, AuthorizeException {
        final EPerson eperson = EPerson.create(context);
        eperson.setFirstName(sn);
        eperson.setLastName(givenName);
        eperson.setEmail(email);
        eperson.setRequireCertificate(false);
        eperson.setSelfRegistered(true);
        eperson.setCanLogIn(true);
        AuthenticationManager.initEPerson(context, request, eperson);
        return eperson;
    }

    

    protected void loadGroups(Context context, Collection<String> roles, HttpServletRequest request) throws SQLException, AuthorizeException {
    	
    	ArrayList<Integer> currentGroups = new ArrayList<>();
    	
    	for (String role : roles) {
            if(DSPACE_ADMIN_ROLE.equals(role)) {
                final Group admins = Group.findByName(context, ADMINISTRATOR_GROUP);

                if (admins != null) {
                    currentGroups.add(admins.getID());
                } else {
                    log.warn(LogManager.getHeader(context, "login", "Could not add user as administrator (group not found)!"));
                }
            } else if(role.startsWith(DSPACE_ROLE_PREFIX)) {
                final String groupName = role.replaceAll(DSPACE_ROLE_PREFIX, "");
                final Group group = Group.findByName(context, groupName);
                if (group != null) {
                    currentGroups.add(group.getID());
                } else {
                    log.warn(LogManager.getHeader(context, "login", "Could not add user to group:" + groupName + " (group not found)!"));
                }
            }
        }

        if(CollectionUtils.isNotEmpty(currentGroups)){
            int[] groupIdArray = ArrayUtils.toPrimitive(currentGroups.toArray(new Integer[currentGroups.size()]));
            request.getSession().setAttribute(SPECIAL_GROUP_REQUEST_ATTRIBUTE, groupIdArray);
        }
    }
    
    protected class DSpaceJerseyBasedOAuthIdentityService extends JerseyBasedOAuthIdentityService {

        private static final String LOGIN_URL = BASE_PATH + "/login";

        protected URI loginUrl;

        public String login(String username, String password) {
            ClientRequest clientRequest = ClientRequest.create().build(
                    UriBuilder.fromUri(this.loginUrl).queryParam(USERNAME_QUERY_PARAM, username)
                            .queryParam(PASSWORD_QUERY_PARAM, password).build(), DEFAULT_METHOD);
            String token = null;
            ClientResponse clientResponse = getClientResponse(clientRequest, getOAuthParameters(""), getOAuth2LeggedSecrets());
            if (clientResponse.getStatus() == HttpServletResponse.SC_OK) {
                token = getTokenId(clientResponse);
            }
            return token;
        }


        public void setUrl(String serverUrl) {
            super.setServerUrl(serverUrl);
            try {
                this.loginUrl = new URI(serverUrl + LOGIN_URL);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    @Override
    public int[] getSpecialGroups(Context context, HttpServletRequest request) throws SQLException {
        int[] groupIds = (int[]) request.getSession().getAttribute(SPECIAL_GROUP_REQUEST_ATTRIBUTE);

        if(ArrayUtils.isNotEmpty(groupIds)){
            return groupIds;
        }

        return new int[0];
    }
}
