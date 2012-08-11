package com.github.jesperfj.tomcat;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;


public class HttpsRedirectValve extends ValveBase {

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        if("true".equals(System.getProperty("https_redirect")) &&
           !"https".equals(request.getHeader("X-Forwarded-Proto"))) {
                StringBuffer uri = request.getRequestURL();
                if(request.getQueryString()!=null) {
                    uri.append("?"+request.getQueryString());
                }
                response.sendRedirect("https"+uri.substring(4));
        } else {
            next.invoke(request, response);
        }
    }

}
