package com.github.jesperfj.tomcat;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;


public class HttpsRedirectValve extends ValveBase {

    private static final Log log = LogFactory.getLog(HttpsRedirectValve.class);

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        if("true".equals(System.getProperty("https_redirect"))) {
            final String proto = request.getHeader("X-Forwarded-Proto");
            log.info("Protocol: "+proto);
        }
        
    }

}
