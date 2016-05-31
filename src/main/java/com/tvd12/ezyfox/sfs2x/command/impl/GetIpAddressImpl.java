package com.tvd12.ezyfox.sfs2x.command.impl;

import com.smartfoxserver.v2.api.ISFSApi;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.extensions.ISFSExtension;
import com.tvd12.ezyfox.core.command.GetIpAddress;
import com.tvd12.ezyfox.core.model.ApiBaseUser;
import com.tvd12.ezyfox.sfs2x.content.impl.AppContextImpl;

/**
 * @author tavandung12
 * Created on May 27, 2016
 *
 */
public class GetIpAddressImpl extends BaseCommandImpl implements GetIpAddress {

    private String username;
    
    /**
     * @param context
     * @param api
     * @param extension
     */
    public GetIpAddressImpl(AppContextImpl context, ISFSApi api, ISFSExtension extension) {
        super(context, api, extension);
    }

    /* (non-Javadoc)
     * @see com.tvd12.ezyfox.core.command.BaseCommand#execute()
     */
    @SuppressWarnings("unchecked")
    @Override
    public String execute() {
        User sfsUser = CommandUtil.getSfsUser(username, api);
        return (sfsUser == null) ? "" : sfsUser.getIpAddress();
    }

    /* (non-Javadoc)
     * @see com.tvd12.ezyfox.core.command.GetIpAddress#user(com.tvd12.ezyfox.core.model.ApiBaseUser)
     */
    @SuppressWarnings("unchecked")
    @Override
    public GetIpAddress user(ApiBaseUser user) {
        this.username = user.getName();
        return this;
    }

    /* (non-Javadoc)
     * @see com.tvd12.ezyfox.core.command.GetIpAddress#user(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public GetIpAddress user(String username) {
        this.username = username;
        return this;
    }

}
