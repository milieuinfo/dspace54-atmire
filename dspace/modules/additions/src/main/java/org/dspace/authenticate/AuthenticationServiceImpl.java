/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.*;

/**
 * Access point for the stackable authentication methods.
 * <p>
 * This class initializes the "stack" from the DSpace configuration,
 * and then invokes methods in the appropriate order on behalf of clients.
 * <p>
 * See the AuthenticationMethod interface for details about what each
 * function does.
 * <p>
 * <b>Configuration</b><br>
 * The stack of authentication methods is defined by one property in the DSpace configuration:
 * <pre>
 *   plugin.sequence.org.dspace.eperson.AuthenticationMethod = <em>a list of method class names</em>
 *     <em>e.g.</em>
 *   plugin.sequence.org.dspace.eperson.AuthenticationMethod = \
 *       org.dspace.eperson.X509Authentication, \
 *       org.dspace.eperson.PasswordAuthentication
 * </pre>
 * <p>
 * The "stack" is always traversed in order, with the methods
 * specified first (in the configuration) thus getting highest priority.
 *
 * @author Larry Stone
 * @version $Revision$
 * @see AuthenticationMethod
 */
public class AuthenticationServiceImpl implements AuthenticationService, InitializingBean {
    /**
     * List of authentication methods, highest precedence first.
     */
    protected List<AuthenticationMethod> methodStack;

    /**
     * SLF4J logging category
     */
    private final Logger log = (Logger) LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    protected AuthenticationServiceImpl() {

    }

    /**
     * @param iKnowWhatImDoing  must be true otherwise all warranties are void
     */
    public AuthenticationServiceImpl(boolean iKnowWhatImDoing) {
        if (iKnowWhatImDoing) {
            afterPropertiesSet();
        }
    }

    @Override
    public void afterPropertiesSet() {
        this.methodStack = new LinkedList<>();
        AuthenticationMethod[] methodStack = (AuthenticationMethod[]) PluginManager.getPluginSequence("authentication", AuthenticationMethod.class);
        Collections.addAll(this.methodStack, methodStack);
    }

    @Override
    public int authenticate(Context context, String username, String password, String realm, HttpServletRequest request) {
        return authenticateInternal(context, username, password, realm, request, false);
    }

    @Override
    public int authenticateImplicit(Context context, String username, String password, String realm, HttpServletRequest request) {
        return authenticateInternal(context, username, password, realm, request, true);
    }

    protected int authenticateInternal(Context context, String username, String password, String realm, HttpServletRequest request, boolean implicitOnly) {
        // better is lowest, so start with the highest.
        int bestRet = AuthenticationMethod.BAD_ARGS;

        // return on first success, otherwise "best" outcome.
        for (AuthenticationMethod aMethodStack : methodStack) {
            if (!implicitOnly || aMethodStack.isImplicit()) {
                int ret = 0;
                try {
                    ret = aMethodStack.authenticate(context, username, password, realm, request);
                } catch (SQLException e) {
                    ret = AuthenticationMethod.NO_SUCH_USER;
                }
                if (ret == AuthenticationMethod.SUCCESS) {
                    EPerson me = context.getCurrentUser();
                    me.setLastActive(new Date());
                    try {
                        me.update();
                    } catch (SQLException ex) {
                        log.error("Could not update last-active stamp", ex);
                    } catch (AuthorizeException ex) {
                        log.error("Could not update last-active stamp", ex);
                    }
                    return ret;
                }
                if (ret < bestRet) {
                    bestRet = ret;
                }
            }
        }
        return bestRet;
    }

    @Override
    public boolean canSelfRegister(Context context, HttpServletRequest request, String username)
            throws SQLException {
        for (int i = 0; i < methodStack.size(); ++i) {
            if (methodStack.get(i).canSelfRegister(context, request, username)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allowSetPassword(Context context, HttpServletRequest request, String username)
            throws SQLException {
        for (int i = 0; i < methodStack.size(); ++i) {
            if (methodStack.get(i).allowSetPassword(context, request, username)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void initEPerson(Context context, HttpServletRequest request, EPerson eperson)
            throws SQLException {
        for (AuthenticationMethod method : methodStack) {
            method.initEPerson(context, request, eperson);
        }
    }

    @Override
    public List<Group> getSpecialGroups(Context context, HttpServletRequest request)
            throws SQLException {
        List<Group> result = new ArrayList<>();

        for (AuthenticationMethod aMethodStack : methodStack) {
            int[] specialGroupIDs = aMethodStack.getSpecialGroups(context, request);
            if (specialGroupIDs.length > 0) {
                for (int specialGroupID : specialGroupIDs) {
                    Group group = Group.find(context, specialGroupID);
                    result.add(group);
                }
            }
        }

        return result;
    }

    @Override
    public Iterator<AuthenticationMethod> authenticationMethodIterator() {
        return methodStack.iterator();
    }
}
