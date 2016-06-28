/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import be.milieuinfo.security.openam.api.OpenAMUserdetails;
import be.milieuinfo.security.openam.oauth.JerseyBasedOAuthIdentityService;
import be.milieuinfo.security.openam.oauth.OAuthTokenPair;

import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

public abstract class OpenAMAuthentication implements AuthenticationMethod {

    private static Logger log = Logger.getLogger(OpenAMAuthentication.class);

    private static final String ADMINISTRATOR_GROUP = "Administrator";
    private String dSpaceAdminRole;
    private String dSpaceRolePrefix;

    protected DSpaceJerseyBasedOAuthIdentityService openAMIdentityService;

    protected OpenAMAuthentication() {
        final String openamServerUrl = ConfigurationManager.getProperty("authentication-openam", "openam.server.url");
        final String consumerToken = ConfigurationManager.getProperty("authentication-openam", "openam.consumer.token");
        final String consumerSecret = ConfigurationManager.getProperty("authentication-openam", "openam.consumer.secret");
        dSpaceRolePrefix = ConfigurationManager.getProperty("authentication-openam", "openam.role.prefix");
        dSpaceAdminRole = ConfigurationManager.getProperty("authentication-openam", "openam.admin.role");

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
                        final EPerson knownEPerson = EPerson.findByEmail(context, email);
                        if (knownEPerson == null) {
                            // TEMPORARILY turn off authorisation
                            context.turnOffAuthorisationSystem();
                            final EPerson eperson = createEPerson(context, request, email, sn, givenName);
                            eperson.update();
                            fixGroups(context, roles, eperson);
                            context.commit();
                            context.restoreAuthSystemState();
                            context.setCurrentUser(eperson);
                            log.info(LogManager.getHeader(context, "login", "type=openam-interactive"));
                            return SUCCESS;
                        } else {
                        	fixGroups(context, roles, knownEPerson);
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

    

    protected void fixGroups(Context context, Collection<String> roles , EPerson ePerson) throws SQLException, AuthorizeException{
    	
    	ArrayList<Group> currentGroups = new ArrayList<Group>();
    	
    	for (String role : roles) {
            if(dSpaceAdminRole.equals(role)) {
                final Group admins = Group.findByName(context, ADMINISTRATOR_GROUP);
                if (admins != null) {
                    admins.addMember(ePerson);
                    admins.update();
                    
                    currentGroups.add(admins);
                    
                } else {
                    log.warn(LogManager.getHeader(context, "login", "Could not add user as administrator (group not found)!"));
                }
            } else if(role.startsWith(dSpaceRolePrefix)) {
                final String groupName = role.replaceAll(dSpaceRolePrefix, "");
                final Group group = Group.findByName(context, groupName);
                if (group != null) {
                    group.addMember(ePerson);
                    group.update();
                    
                    currentGroups.add(group);
                    
                } else {
                    log.warn(LogManager.getHeader(context, "login", "Could not add user to group:" + groupName + " (group not found)!"));
                }
            }
        }
    	
    	
    	Group[] dbGroups = Group.allMemberGroups(context, ePerson);
    	for (Group dbGroup : dbGroups ){
    		if (dbGroup.getID() == 0 ){
    			log.debug("Everybody belongs to the anonymous group");
    		}else if (!currentGroups.contains(dbGroup)){
    			log.info(ePerson.getName() + " belongs to group: "+ dbGroup.getName() + " in the database but not in LDAP, removing person from group");
    			dbGroup.removeMember(ePerson);
    			dbGroup.update();
    		}
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

}
