/**
 * 
 */
package com.tvd12.ezyfox.sfs2x.command.impl;

import java.util.HashMap;
import java.util.Map;

import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.api.ISFSApi;
import com.smartfoxserver.v2.api.ISFSBuddyResponseApi;
import com.smartfoxserver.v2.buddylist.BuddyList;
import com.smartfoxserver.v2.buddylist.BuddyListManager;
import com.smartfoxserver.v2.buddylist.SFSBuddyEventParam;
import com.smartfoxserver.v2.controllers.SystemRequest;
import com.smartfoxserver.v2.core.ISFSEventManager;
import com.smartfoxserver.v2.core.ISFSEventParam;
import com.smartfoxserver.v2.core.SFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.exceptions.SFSBuddyListException;
import com.smartfoxserver.v2.extensions.ISFSExtension;
import com.tvd12.ezyfox.core.command.AddBuddy;
import com.tvd12.ezyfox.core.config.APIKey;
import com.tvd12.ezyfox.core.model.ApiBaseUser;
import com.tvd12.ezyfox.core.model.ApiBuddy;
import com.tvd12.ezyfox.core.model.ApiUser;
import com.tvd12.ezyfox.core.model.ApiZone;
import com.tvd12.ezyfox.sfs2x.content.impl.AppContextImpl;
import com.tvd12.ezyfox.sfs2x.model.impl.ApiBuddyImpl;

/**
 * @author tavandung12
 *
 */
public class AddBuddyImpl extends BaseCommandImpl implements AddBuddy {

    private String owner;
    private String target;
    private boolean temp = false;
    private boolean fireClientEvent = true;
    private boolean fireServerEvent = true;
    
    protected ApiZone zone;
    
    public AddBuddyImpl(AppContextImpl context, ISFSApi api, ISFSExtension extension) {
        super(context, api, extension);
    }
    
    /* (non-Javadoc)
     * @see com.tvd12.ezyfox.core.command.AddBuddy#owner(com.tvd12.ezyfox.core.model.ApiBaseUser)
     */
    @SuppressWarnings("unchecked")
    @Override
    public AddBuddy owner(ApiBaseUser owner) {
        this.owner = owner.getName();
        return this;
    }



    /* (non-Javadoc)
     * @see com.tvd12.ezyfox.core.command.AddBuddy#owner(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public AddBuddy owner(String ownerName) {
        this.owner = ownerName;
        return this;
    }



    /* (non-Javadoc)
     * @see com.tvd12.ezyfox.core.command.AddBuddy#zone(com.tvd12.ezyfox.core.model.ApiZone)
     */
    @SuppressWarnings("unchecked")
    @Override
    public AddBuddy zone(ApiZone zone) {
        this.zone = zone;
        return this;
    }



    /* (non-Javadoc)
     * @see com.tvd12.ezyfox.core.command.AddBuddy#buddy(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public AddBuddy buddy(String buddyName) {
        this.target = buddyName;
        return this;
    }



    /* (non-Javadoc)
     * @see com.tvd12.ezyfox.core.command.AddBuddy#temp(boolean)
     */
    @SuppressWarnings("unchecked")
    @Override
    public AddBuddy temp(boolean isTemp) {
        this.temp = isTemp;
        return this;
    }



    /* (non-Javadoc)
     * @see com.tvd12.ezyfox.core.command.AddBuddy#fireClientEvent(boolean)
     */
    @SuppressWarnings("unchecked")
    @Override
    public AddBuddy fireClientEvent(boolean fireClientEvent) {
        this.fireClientEvent = fireClientEvent;
        return this;
    }



    /* (non-Javadoc)
     * @see com.tvd12.ezyfox.core.command.AddBuddy#fireServerEvent(boolean)
     */
    @SuppressWarnings("unchecked")
    @Override
    public AddBuddy fireServerEvent(boolean fireServerEvent) {
        this.fireServerEvent = fireServerEvent;
        return this;
    }
    
    @SuppressWarnings("unchecked")
    public ApiBuddy execute() {
        User sfsOwner = api.getUserByName(owner);
        ApiUser targetUser = getUser(target);
        ApiUser ownerUser = (ApiUser) sfsOwner.getProperty(APIKey.USER);
        ISFSBuddyResponseApi responseAPI = SmartFoxServer.getInstance().getAPIManager()
                .getBuddyApi().getResponseAPI();
        ISFSEventManager eventManager = SmartFoxServer.getInstance().getEventManager();
        BuddyList buddyList = sfsOwner.getZone()
                .getBuddyListManager().getBuddyList(owner);
        BuddyListManager buddyListManager = sfsOwner.getZone().getBuddyListManager();
        checkBuddyManagerIsActive(buddyListManager, sfsOwner);
        sfsOwner.updateLastRequestTime();
        ApiBuddyImpl buddy = new ApiBuddyImpl(target, temp);
        buddy.setOwner(ownerUser);
        buddy.setParentBuddyList(buddyList);
        if(targetUser != null)  buddy.setUser(targetUser);
        try {
            buddyList.addBuddy(buddy);
            if (fireClientEvent)
                responseAPI.notifyAddBuddy(buddy, sfsOwner);
            if (fireServerEvent) {
                Map<ISFSEventParam, Object> evtParams = new HashMap<>();
                evtParams.put(SFSEventParam.ZONE, sfsOwner.getZone());
                evtParams.put(SFSEventParam.USER, sfsOwner);
                evtParams.put(SFSBuddyEventParam.BUDDY, buddy);
                eventManager.dispatchEvent(new SFSEvent(SFSEventType.BUDDY_ADD, evtParams));
            }
        } catch (SFSBuddyListException e) {
            if (fireClientEvent) {
                api.getResponseAPI().notifyRequestError(e, sfsOwner, SystemRequest.AddBuddy);
            }
            return null;
        }
        return buddy;
    }
    
    /**
     * @param buddyListManager
     * @param sfsOwner
     */
    private void checkBuddyManagerIsActive(BuddyListManager buddyListManager, User sfsOwner) {
        if (!buddyListManager.isActive()) {
            throw new IllegalStateException(
                    String.format("BuddyList operation failure. BuddyListManager is not active. Zone: %s, Sender: %s", new Object[] { sfsOwner.getZone(), sfsOwner }));
          }
    }

    public ApiUser getUser(String name) {
        User sfsUser = CommandUtil.getSfsUser(name, api);
        if(sfsUser == null)
            return null;
        return (ApiUser) sfsUser.getProperty(APIKey.USER);
    }
}