package com.github.jesperfj.tomcat;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.Base64;
import org.apache.catalina.valves.ValveBase;


public class BasicAuthValve extends ValveBase {

    static final String base64Auth;
    
    static {
        if(System.getProperty("basic_auth")!=null) {
            base64Auth = Base64.encode(System.getProperty("basic_auth").getBytes());
        } else {
            base64Auth = null;
        }
    }
    
    
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        if(base64Auth!=null) {
            if(request.getHeader("Authorization")!=null) {
                final String[] f  = request.getHeader("Authorization").split(" ",2);
                if(f[0]!=null && f[0].equals("Basic")) {
                    if(f[1]!=null && f[1].equals(base64Auth)) {
                        getNext().invoke(request, response);
                        return;
                    }
                }
            }
            sendChallenge(response);
            return;
        } else {
            getNext().invoke(request, response);
        }
    }        
    
    private void sendChallenge(Response response) throws IOException, ServletException {
        response.addHeader("WWW-Authenticate", "Basic realm=\"Requires Authentication\"");
        response.sendError(Response.SC_UNAUTHORIZED);
    }

}
