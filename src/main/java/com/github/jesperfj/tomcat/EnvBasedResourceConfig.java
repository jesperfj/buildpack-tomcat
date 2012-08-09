package com.github.jesperfj.tomcat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.deploy.ContextResource;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Configures external services specified in the OS environment as JNDI resources.
 * 
 * It is common to use OS environment variables to specify connection information for
 * external services used by a web application. OS environment variables are convenient
 * because they are universal and they inherently tie to the environment of the app,
 * not the source code. This makes it easy to use different configurations for different
 * deployments of the same code base (e.g. staging and production).
 * 
 * This class is a Tomcat Context Listener. It will execute during startup of Tomcat when
 * the web application context is set up.
 * 
 * The class looks for a DATABASE_URL environment variable. If it doesn't exist, nothing
 * is done. If it is found, a JNDI data source will be configured based on the connection
 * settings in the DATABASE_URL variable. The value must be formatted as
 * 
 * <code>postgres://username:password@host:port/database</code>
 * 
 * port is optional. Only postgres databases are supported. The driver will be
 * org.postgresql.Driver and the driver classes are expected to be
 * on the classpath. You accomplish this by adding it to CATALINA_HOME/lib. The default
 * configuration with a DATABASE_URL like the one above corresponds to the following
 * XML configuration:
 * 
 * <code>
 * <Resource name="jdbc/default" auth="Container"
 *          type="javax.sql.DataSource" driverClassName="org.postgresql.Driver"
 *          url="jdbc:postgresql://host:post/database"
 *          username="username" password="password" maxActive="20" maxIdle="10"
 *          maxWait="-1"/>
 * </code>
 * 
 * You can customize the JNDI name and the connection properties <code>maxActive</code>, 
 * <code>maxIdle</code>, and <code>maxWait</code> by setting another environment variable
 * like so:
 * 
 * <code>DATABASE_JNDI={"name": "jdbc/yourdb", "maxWait": "5", "maxIdle": "10", "maxActive": "15"}</code>
 * 
 * The variable is parsed as a JSON object and the properties will override the default
 * settings.
 * 
 * 
 * @author jesperfj
 *
 */
public class EnvBasedResourceConfig implements LifecycleListener {

    private static final Log log = LogFactory.getLog(EnvBasedResourceConfig.class);

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (event.getType().equals(Lifecycle.BEFORE_START_EVENT)) {

            String dbstr = System.getenv("DATABASE_URL");
            if(dbstr==null) {
                log.info("DATABASE_URL not set in environment. No data souce will be configured");
                return;
            }

            try {

                Context context = (Context) event.getLifecycle();
                
                // TODO:
                //
                // It should loop through all DATABASE_<NAME>_URL variables and configure them. For each
                // it should use DATABASE_<NAME>_JNDI for custom config (e.g. custom JNDI name).
                //
                // How to support many different types of services in a modular way:
                //
                // It feels like the best approach is to just write more listeners and add each of them
                // to the context config. It should be a fairly smooth process for an external service
                // provider to fork this repo add a class for their service and add a line in ROOT.xml.
                // Then test and send a pull request.
                //
                // Instead of checking code into this repo, we could also run the build at assembly
                // time to pull in jars for different external services from Maven central. This would
                // make it easier to maintain the code. We may not be able to tell exactly what services
                // to configure since we don't know the runtime environment at build time.

                URI dburi = new URI(System.getenv("DATABASE_URL"));
                if(!dburi.getScheme().equals("postgres")) {
                    log.info("I only know how to configure postgres databases. "+
                             "Don't know how to configure a data source for this URL: "+dbstr+
                             ". That doesn't mean it can't be done. I suggest forking the buildpack and hacking it on your own.");
                    return;
                }

                String url = "jdbc:postgresql://"+dburi.getHost()+(dburi.getPort()!=-1? ":"+dburi.getPort() : "") + dburi.getPath();
                String username = dburi.getUserInfo().split(":")[0];
                String password = dburi.getUserInfo().split(":")[1];

                @SuppressWarnings("serial")
                Map<String,String> config = new HashMap<String,String>() {
                    {
                        put("maxActive", "20");
                        put("maxIdle", "10");
                        put("maxWait", "-1");
                        put("name", "jdbc/default");
                    }
                };
                
                String jndiConfigJson = System.getenv("DATABASE_JNDI");
                if(jndiConfigJson!=null) {
                    JSONObject jndiConfig = new JSONObject(jndiConfigJson);
                    for(String s : config.keySet()) {
                        try {
                            config.put(s,jndiConfig.getString(s));
                        }
                        catch (JSONException e) {}
                    }
                }
                ContextResource c = new ContextResource();

                c.setAuth("Container");
                c.setType("javax.sql.DataSource");
                c.setName(config.get("name"));
                c.setProperty("username", username);
                c.setProperty("password", password);
                c.setProperty("url", url);
                c.setProperty("driverClassName", "org.postgresql.Driver");
                c.setProperty("maxActive", config.get("maxActive"));
                c.setProperty("maxIdle", config.get("maxIdle"));
                c.setProperty("maxWait", config.get("maxWait"));

                context.getNamingResources().addResource(c);

                log.info("Configured JNDI data source from environment: $DATABASE_URL => "+
                            config.get("name")+
                            ": url="+url+
                            ", username="+username+
                            ", config="+config);
            } catch (URISyntaxException e) {
                log.error("Could not configure a JNDI data source from DATABASE_URL. Invalid URL: "+System.getenv("DATABASE_URL"));
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

}
