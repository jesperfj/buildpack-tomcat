package com.github.jesperfj.tomcat;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;


public class EnvBasedResourceConfig implements LifecycleListener {

    private static final Log log = LogFactory.getLog( EnvBasedResourceConfig.class );

    private Context context;
    
    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        try {
            context = (Context) event.getLifecycle();
        } catch (ClassCastException e) {
            log.error("Couldn't cast lifecycle to Context class.", e);
            return;
        }
        if (event.getType().equals(Lifecycle.AFTER_INIT_EVENT)) {
            log.info("Got after init event. Getting ready to configure resources from environment");
        }
    }

}
