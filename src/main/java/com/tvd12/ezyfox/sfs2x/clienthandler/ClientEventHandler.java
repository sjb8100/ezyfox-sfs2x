package com.tvd12.ezyfox.sfs2x.clienthandler;

import java.lang.reflect.Method;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.tvd12.ezyfox.core.constants.APIKey;
import com.tvd12.ezyfox.core.content.impl.BaseContext;
import com.tvd12.ezyfox.core.entities.ApiUser;
import com.tvd12.ezyfox.core.exception.BadRequestException;
import com.tvd12.ezyfox.core.reflect.ReflectMethodUtil;
import com.tvd12.ezyfox.core.serialize.ObjectDeserializer;
import com.tvd12.ezyfox.core.structure.RequestResponseClass;
import com.tvd12.ezyfox.core.util.UserAgentUtil;
import com.tvd12.ezyfox.sfs2x.data.impl.ParamTransformer;
import com.tvd12.ezyfox.sfs2x.data.impl.SfsParameters;
import com.tvd12.ezyfox.sfs2x.serializer.RequestParamDeserializer;
import com.tvd12.ezyfox.sfs2x.util.AgentUtil;

/**
 * 
 * This class handle request from client and notify that request to all listener that it manages
 * 
 * @author tavandung12
 * Created on May 31, 2016
 *
 */

public class ClientEventHandler extends ClientRequestHandler {
	
    /**
     * @param context application context
     * @param command request's command
     */
	public ClientEventHandler(BaseContext context, String command) {
		super(context, command);
	}

	/**
	 * Handle request from client
	 */
	@Override
	public void handleClientRequest(User user, ISFSObject params) {
		try {
			debugLogRequestInfo(user, params);
			ApiUser apiUser = getUserAgent(user);
	        for(RequestResponseClass clazz : listeners) {
	            Object userAgent = checkUserAgent(clazz, apiUser);
	            notifyListener(clazz, params, user, userAgent);
	        }
		} catch(Exception e) {
			processHandlerException(e, user);
		}
	}
	
	protected void processHandlerException(Exception e, User user) {
		if(isBadRequestException(e)) { 
	    	getLogger().debug("handle client request error", e);
	        responseErrorToClient(e, user);
	    }
	    else {
	    	getLogger().error("handle client request error", e);
	    	throw new RuntimeException(e);
	    }
	}
	
	protected void debugLogRequestInfo(User user, ISFSObject params) {
		getLogger().debug("user {} request command = {}, data = {}", 
				user.getName(), command, paramsToJson(params));
	}
	
	protected String paramsToJson(ISFSObject params) {
		return params.toJson();
	}
	
	protected ApiUser getUserAgent(User user) {
	    return AgentUtil.getUserAgent(user);
	}
	
	/**
	 * Notify all listeners
	 * 
	 * @param clazz structure of listener class
	 * @param params request parameters
	 * @param user request user
	 * @param userAgent user agent's object
	 * @throws Exception when has any error from listener
	 */
	private void notifyListener(RequestResponseClass clazz, 
	        ISFSObject params, User user, Object userAgent) throws Exception {
	    Object listener = clazz.newInstance();
	    setDataToListener(clazz, listener, params);
	    invokeExecuteMethod(clazz.getExecuteMethod(), listener, userAgent);
	    responseClient(clazz, listener, user);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
    private void setDataToListener(RequestResponseClass clazz, 
	        Object listener, ISFSObject params) {
	    try {
	        ObjectDeserializer deserializer = 
	            context.getObjectDeserializer(listener.getClass());
	        deserializer.deserialize(listener, new SfsParameters(params));
	    } catch(IllegalArgumentException e) {
	        new RequestParamDeserializer()
                .deserialize(clazz.getRequestListenerClass(), params, listener);
	    }
	    
	}
	
	/**
	 * Invoke the execute method
	 * 
	 * @param method the execute method
	 * @param listener the listener
	 * @param userAgent the user agent object
	 */
	protected void invokeExecuteMethod(Method method, Object listener, Object userAgent) {
	    ReflectMethodUtil.invokeExecuteMethod(
                method, listener, context, userAgent);
	}
	
	/**
	 * For each listener, we may use a class of user agent, so we need check it 
	 * 
	 * @param clazz structure of listener class
	 * @param userAgent user agent object
	 * @return instance of user agent
	 */
	private Object checkUserAgent(RequestResponseClass clazz, ApiUser userAgent) {
        if(clazz.getUserClass().isAssignableFrom(userAgent.getClass()))
            return userAgent;
        return UserAgentUtil.getGameUser(userAgent, clazz.getUserClass());
    }
	
	/**
	 * Response information to client
	 * 
	 * @param clazz structure of listener class
	 * @param listener listener object
	 * @param user smartfox user
	 */
	private void responseClient(RequestResponseClass clazz, Object listener, User user) {
		if(!clazz.isResponseToClient()) return;
		String command = clazz.getResponseCommand();
		ISFSObject params = (ISFSObject) new ParamTransformer(context)
		        .transform(listener).getObject();
		send(command, params, user);
	}
	
	/**
	 * Check if has any listeners throw a BadRequestException
	 * 
	 * @param e exception
	 * @return true or false
	 */
	private boolean isBadRequestException(Exception e) {
	    return ExceptionUtils.indexOfThrowable(e, BadRequestException.class) != -1;
	}
	
	/**
	 * Response error to client
	 * 
	 * @param ex the exception
	 * @param user the recipient
	 * 
	 */
	private void responseErrorToClient(Exception ex, User user) {
	    BadRequestException e = getBadRequestException(ex);
	    if(!e.isSendToClient())    return;
	    ISFSObject params = new SFSObject();
	    params.putUtfString(APIKey.MESSAGE, e.getReason());
	    params.putInt(APIKey.CODE, e.getCode());
	    send(APIKey.ERROR, params, user);
	}
	
	/**
	 * Get BadRequestException from the exception
	 * 
	 * @param ex the exception
	 * @return BadRequestException
	 */
	private BadRequestException getBadRequestException(Exception ex) {
	    return (BadRequestException) ExceptionUtils
	            .getThrowables(ex)[ExceptionUtils.indexOfThrowable(ex, BadRequestException.class)];
	}
	
	/* (non-Javadoc)
	 * @see com.smartfoxserver.v2.extensions.BaseClientRequestHandler#send(java.lang.String, com.smartfoxserver.v2.entities.data.ISFSObject, com.smartfoxserver.v2.entities.User)
	 */
	@Override
	protected void send(String cmd, ISFSObject params, User recipient) {
		super.send(cmd, params, recipient);
		logResponse(cmd, params);
	}
	
	protected void logResponse(String cmd, ISFSObject params) {
		getLogger().debug("response command: {} with data {}", cmd, params.toJson());
	}
}
