package org.bonitasoft.store.rest;

import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.store.source.git.RESTResultKeyValueMap;

/**
 * This class reflects the information for a REST request.
 */
public class RESTRequest {

    /** the Header URL */
    public String headerUrl;
    public String uri;

    private URL url;
    /**
     * The REST HTTP Method.
     */
    private RESTHTTPMethod restMethod;

    /**
     * The authorization.
     */
    private Authorization authorization;

    /**
     * The headers.
     */
    private List<Header> listHeaders = new ArrayList<>();
    /**
     * The cookies.
     */
    private List<HttpCookie> cookies = new ArrayList<>();

    /**
     * The ssl information.
     */
    private SSL ssl;

    /**
     * Is the request has to follow the redirections.
     */
    private boolean redirect;

    /**
     * Is the response body has to be digested.
     */
    private boolean ignore = false;
    /**
     * return the response as String
     */
    private boolean isStringOutput = true;

    /**
     * The content information.
     */
    private Content content = null;

    /**
     * The body string.
     */
    private String body = "";

    public List<BEvent> listEvents = new ArrayList<BEvent>();

    /**
     * URL value getter.
     * 
     * @return The URL value.
     * @throws MalformedURLException 
     */
    public URL getUrl()  {
        
        return url;
    }

    public void calculateUrlFromUri() throws MalformedURLException {
        url = new URL(headerUrl+uri);
    }
    /**
     * The URL value setter.
     * 
     * @param url URL value.
     */
    public void setUrl(final URL url) {
        this.url = url;
    }

    /**
     * RESTHTTPMethod value getter.
     * 
     * @return The RESTHTTPMethod value.
     */
    public RESTHTTPMethod getRestMethod() {
        return restMethod;
    }

    /**
     * RESTHTTPMethod value setter.
     * 
     * @param restMethod The RESTHTTPMethod new value.
     */
    public void setRestMethod(final RESTHTTPMethod restMethod) {
        this.restMethod = restMethod;
    }

    /**
     * Authorization value getter.
     * 
     * @return The authorization value.
     */
    public Authorization getAuthorization() {
        return authorization;
    }

    /**
     * Authorization value setter.
     * 
     * @param authorization The authorization new value.
     */
    public void setAuthorization(final Authorization authorization) {
        this.authorization = authorization;
    }

    /**
     * SSL value getter.
     * 
     * @return The SSL value.
     */
    public SSL getSsl() {
        return ssl;
    }

    /**
     * SSL value setter.
     * 
     * @param ssl The SSL new value.
     */
    public void setSsl(final SSL ssl) {
        this.ssl = ssl;
    }

    /**
     * Redirect value getter.
     * 
     * @return The redirect value.
     */
    public boolean isRedirect() {
        return redirect;
    }

    /**
     * Redirect value setter.
     * 
     * @param redirect The redirect new value.
     */
    public void setRedirect(final boolean redirect) {
        this.redirect = redirect;
    }

    /**
     * Ignore value getter.
     * 
     * @return The ignore value.
     */
    public boolean isIgnore() {
        return ignore;
    }

    public boolean isStringOutput() {
        return isStringOutput;
    }

    public void setStringOutput(final boolean isStringOutput) {
        this.isStringOutput = isStringOutput;
    }

    /**
     * Ignore value setter.
     * 
     * @param ignore The ignore new value.
     */
    public void setIgnore(final boolean ignore) {
        this.ignore = ignore;
    }

    /**
     * Headers value getter.
     * 
     * @return The headers value.
     */
    public List<Header> getHeaders() {
        return listHeaders;
    }

    /**
     * Cookies value getter.
     * 
     * @return The cookies value.
     */
    public List<HttpCookie> getCookies() {
        return cookies;
    }

    /**
     * Add a header couple in the headers.
     * 
     * @param key The key of the new header.
     * @param value The lonely value of the new header.
     * @return True if the header has been added or false otherwise.
     */
    public void addHeader(final String key, final String value) {
       final Header header = new BasicHeader(key, value);
       listHeaders.add( header );
       /*
            final RESTResultKeyValueMap restResultKeyValueMap = new RESTResultKeyValueMap();
            restResultKeyValueMap.setKey(key);
            final List<String> values = new ArrayList<String>();
            values.add(value);
            restResultKeyValueMap.setValue(values);
            headers.add(restResultKeyValueMap);
            return true;
        }
        return false;
        */
    }

    public void addHeader( Header header ) {
        listHeaders.add( header );
    }
    public void addHeaders( List<Header> listHeader ) {
        this.listHeaders.addAll( listHeader );
    }
    /**
     * Add a header couple in the headers.
     * 
     * @param key The key of the new header.
     * @param value The list of values of the new header.
     * @return True if the header has been added or false otherwise.
     */
    public void addHeader(final String key, final List<String> values) {
        for (String aValue : values ) {
            final Header header = new BasicHeader(key, aValue);
            listHeaders.add( header );
        }
   
    }

    /**
     * Add a cookie couple in the cookies.
     * 
     * @param key The key of the new cookie.
     * @param value The lonely value of the new cookie.
     * @return True if the cookie has been added or false otherwise.
     */
    public boolean addCookie(final String key, final String value) {
        if (cookies == null) 
            cookies = new ArrayList<>();
            
        final HttpCookie cookie = new HttpCookie(key, value);
        cookies.add(cookie);
        return true;
    }

    /**
     * Content value getter.
     * 
     * @return The content value.
     */
    public Content getContent() {
        return content;
    }

    /**
     * Content value setter.
     * 
     * @param content The content new value.
     */
    public void setContent(final Content content) {
        this.content = content;
    }

    /**
     * Body value getter.
     * 
     * @return The body value.
     */
    public String getBody() {
        return body;
    }

    /**
     * Body value setter.
     * 
     * @param body The body new value.
     */
    public void setBody(final String body) {
        this.body = body;
    }

}
