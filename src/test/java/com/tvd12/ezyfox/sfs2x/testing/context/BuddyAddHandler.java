package com.tvd12.ezyfox.sfs2x.testing.context;

import com.tvd12.ezyfox.core.annotation.ServerEventHandler;
import com.tvd12.ezyfox.core.config.ServerEvent;
import com.tvd12.ezyfox.core.content.AppContext;
import com.tvd12.ezyfox.core.model.ApiBuddy;
import com.tvd12.ezyfox.core.model.ApiZone;

/**
 * @author tavandung12
 * Created on May 30, 2016
 *
 */
@ServerEventHandler(event = ServerEvent.BUDDY_ADD)
public class BuddyAddHandler {

    public void handle(AppContext context, ApiZone zone, ApiBuddy buddy) {
        
    }
    
}
