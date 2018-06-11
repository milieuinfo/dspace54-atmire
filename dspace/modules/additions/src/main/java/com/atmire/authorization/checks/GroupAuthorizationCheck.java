package com.atmire.authorization.checks;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.springframework.beans.factory.annotation.Required;

import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by jonas - jonas@atmire.com on 13/04/16.
 */
public class GroupAuthorizationCheck implements AuthorizationCheck{

    /* Log4j logger*/
    private static final Logger log =  Logger.getLogger(GroupAuthorizationCheck.class);

    private List<String> allowedGroups;

    @PostConstruct
    public void initGroups(){
        Context context = null;
        try {
            context = new Context();
            Group adminGroup = Group.findByName(context,"Administrator");
            context.turnOffAuthorisationSystem();
            for(String group : allowedGroups){
                Group g = Group.findByName(context,group);
                if(g == null){
                    Group createdGroup = Group.create(context);
                    createdGroup.setName(group);
                    createdGroup.addMember(adminGroup);
                    createdGroup.update();
                }
            }
            context.restoreAuthSystemState();
            context.commit();
        } catch (SQLException e) {
           log.error("Error while checking for non-existing groups during the authorization check.",e);
        } catch (AuthorizeException e) {
            log.error(e);
        } finally {
            if(context!=null){
                context.abort();
            }
        }
    }
    @Required
    public void setAllowedGroups(List<String> allowedGroups){
        this.allowedGroups=allowedGroups;
    }

    public List<String> getGroups(){
        return allowedGroups;
    }
    @Override
    public boolean checkAuthorization(Context context,DSpaceObject dso) {
        EPerson ePerson = context.getCurrentUser();

        for(String group:allowedGroups){
            try {
                Group g = Group.findByName(context,group);

               if(g!=null && Group.allMemberIDs(context,g).contains(ePerson.getID())){
                   return true;
                }
            } catch (SQLException e) {
                log.error(e);
            }

        }
        return false;
    }
}
