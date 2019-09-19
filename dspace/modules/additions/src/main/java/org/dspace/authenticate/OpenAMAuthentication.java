/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import be.milieuinfo.security.openam.api.*;
import be.milieuinfo.security.openam.oauth.*;
import com.atmire.authenticate.*;
import com.atmire.eperson.acl.service.*;
import com.sun.jersey.api.client.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import javax.servlet.http.*;
import javax.ws.rs.core.*;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.*;
import org.apache.log4j.*;
import org.dspace.authorize.*;
import org.dspace.core.*;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.*;
import org.dspace.utils.*;

public abstract class OpenAMAuthentication implements AuthenticationMethod {

    private static Logger log = Logger.getLogger(OpenAMAuthentication.class);

    public final static String SPECIAL_GROUP_REQUEST_ATTRIBUTE = "openam.specialgroup";
    private static final String ADMINISTRATOR_GROUP = "Administrator";
    private String dSpaceAdminRole;
    private String dSpaceRolePrefix;

    private EPersonAclMetadataService ePersonAclMetadataService =
            new DSpace().getServiceManager().getServicesByType(EPersonAclMetadataService.class).get(0);

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
                final String givenName = userDetails.getAttributeValue("givenName") == null ? userName : userDetails.getAttributeValue("givenName");

                if (log.isDebugEnabled()) {
                    logOpenAmUserDetails(email, userDetails);
                }

                final Collection<String> roles = userDetails.getRoles();
                if (!StringUtils.isBlank(email)) {
                    log.debug("OpenAM Identify Service authenticated SSO ID " + ssoId + " as user " + email);
                    try {
                        loadGroups(context, roles, request, email);
                        final EPerson knownEPerson = EPerson.findByEmail(context, email);
                        if (knownEPerson == null) {
                            log.debug("Creating new EPerson for SSO ID " + ssoId + " with e-mail " + email);
                            // TEMPORARILY turn off authorisation
                            context.turnOffAuthorisationSystem();
                            final EPerson eperson = createEPerson(context, request, email, sn, givenName);
                            eperson.update();
                            updateEpersonAclMetadata(context, eperson, userDetails);
                            context.commit();
                            context.restoreAuthSystemState();
                            context.setCurrentUser(eperson);

                            log.info(LogManager.getHeader(context, "login", "type=openam-interactive"));
                            return SUCCESS;
                        } else {
                            log.debug("Found existing EPerson with ID " + knownEPerson.getID() + " for SSO ID " + ssoId + " with e-mail " + email);
                            updateEpersonAclMetadata(context, knownEPerson, userDetails);
                            context.setCurrentUser(knownEPerson);
                            return SUCCESS;
                        }
                    } catch (AuthorizeException e) {
                        log.warn(LogManager.getHeader(context, "authorize_exception", ""), e);
                        return BAD_ARGS;
                    }
                } else {
                    log.warn("Received a blank e-mail address from OpenAM Identify Service for SSO ID " + ssoId);
                    return BAD_ARGS;
                }
            } else {
                log.warn("OpenAM Identify Service did not return any user details for SSO ID " + ssoId);
                return NO_SUCH_USER;
            }
        } else {
            if(log.isDebugEnabled()) {
                log.debug("Unable to use OpenAM authentication with a blank SSO ID (ip " + request.getRemoteAddr() + ")");
            }
            return NO_SUCH_USER;
        }
    }

    private void logOpenAmUserDetails(final String email, final OpenAMUserdetails userDetails) {
        Map<String, String[]> attributes = userDetails.getAttributes();

        for (String key : attributes.keySet()) {
            logDebugForUser(email, key + " " + Arrays.asList(attributes.get(key)).toString());
        }
        logDebugForUser(email, "getOrganisatieCode [" + userDetails.getOrganisatieCode() + "]");
        logDebugForUser(email, "getOrganisatieCodeDetail [" + userDetails.getOrganisatieCodeDetail() + "]");
        logDebugForUser(email, "getPersonId [" + userDetails.getPersonId() + "]");
        logDebugForUser(email, "getUsername [" + userDetails.getUsername() + "]");
        logDebugForUser(email, "getAuthLevel [" + userDetails.getAuthLevel() + "]");

        Collection<String> roles = userDetails.getRoles();
        for (String role : roles) {
            logDebugForUser(email, "Role [" + role + "]");
        }
    }

    private void logDebugForUser(final String email, final String s) {
        log.debug("(User " + email + ") " + s);
    }

    private void updateEpersonAclMetadata(Context context, EPerson eperson, OpenAMUserdetails userDetails) {
        try {
            ePersonAclMetadataService.removeAllFields(context, eperson);

            List<OpenAMEpersonMetadataMapper> openAMEpersonMetadataMappers = new DSpace().getServiceManager().getServicesByType(OpenAMEpersonMetadataMapper.class);

            for (OpenAMEpersonMetadataMapper openAMEpersonMetadataMapper : openAMEpersonMetadataMappers) {
                openAMEpersonMetadataMapper.mapToMetadata(context, eperson, userDetails);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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


    protected void loadGroups(Context context, Collection<String> roles, HttpServletRequest request, String email) throws SQLException, AuthorizeException {

        ArrayList<Integer> currentGroups = new ArrayList<>();

        log.info("Number of OpenAM roles received for user " + email + " is " + CollectionUtils.size(roles));

        for (String role : roles) {
            log.info("User " + email + " has OpenAM role " + role);

            if (dSpaceAdminRole.equals(role)) {
                final Group admins = Group.findByName(context, ADMINISTRATOR_GROUP);

                if (admins != null) {
                    currentGroups.add(admins.getID());

                    if (log.isDebugEnabled()) {
                        log.debug("User " + email + " was added to the " + admins.getName() + " group");
                    }

                } else {
                    log.warn(LogManager.getHeader(context, "login", "Could not add user as administrator (group not found)!"));
                }
            } else if (role.startsWith(dSpaceRolePrefix)) {
                final String groupName = role.replaceAll(dSpaceRolePrefix, "");
                final Group group = Group.findByName(context, groupName);
                if (group != null) {
                    currentGroups.add(group.getID());
                    if (log.isDebugEnabled()) {
                        log.debug("User " + email + " was added to the " + group.getName() + " group");
                    }
                } else {
                    log.warn(LogManager.getHeader(context, "login", "Could not add user to group:" + groupName + " (group not found)!"));
                }
            }
        }

        if (CollectionUtils.isNotEmpty(currentGroups)) {
            int[] groupIdArray = ArrayUtils.toPrimitive(currentGroups.toArray(new Integer[currentGroups.size()]));
            request.getSession().setAttribute(SPECIAL_GROUP_REQUEST_ATTRIBUTE, groupIdArray);
        }
    }

    protected class DSpaceJerseyBasedOAuthIdentityService extends JerseyBasedOAuthIdentityService {

        private static final String LOGIN_URL = BASE_PATH + "/login";

        protected URI loginUrl;

        public String login(String username, String password) {
            String token = null;

            if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
                ClientRequest clientRequest = ClientRequest.create().build(
                        UriBuilder.fromUri(this.loginUrl).queryParam(USERNAME_QUERY_PARAM, username)
                                .queryParam(PASSWORD_QUERY_PARAM, password).build(), DEFAULT_METHOD);

                ClientResponse clientResponse = getClientResponse(clientRequest, getOAuthParameters(""), getOAuth2LeggedSecrets());

                if (clientResponse.getStatus() == HttpServletResponse.SC_OK) {

                    if(log.isDebugEnabled()) {
                        try {
                            String entity = IOUtils.toString(clientResponse.getEntityInputStream(), StandardCharsets.UTF_8);
                            log.debug("OpenAM response entity: " + entity);
                            clientResponse.setEntityInputStream(new ByteArrayInputStream(entity.getBytes(StandardCharsets.UTF_8)));
                        } catch (IOException ex) {
                            log.error(ex);
                        }
                    }

                    token = getTokenId(clientResponse);
                } else {
                    log.warn("OpenAM service " + loginUrl + " returned status code " + clientResponse.getStatus() + " for user " + username);
                }
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
        if(request != null && request.getSession() != null) {
            int[] groupIds = (int[]) request.getSession().getAttribute(SPECIAL_GROUP_REQUEST_ATTRIBUTE);

            if (ArrayUtils.isNotEmpty(groupIds)) {
                return groupIds;
            }
        }

        return new int[0];
    }

}
