package org.bonitasoft.store.rest;

import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicHeader;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.store.rest.CollectOutput.POLICYOUTPUT;

/**
 * This class reflects the information for a REST request.
 */
public class RESTRequest {

    /** the Header URL */
    private String headerUrl;
    private String uri;

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
     * the same header may be multiple in the list.
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
    private CollectOutput collectOutput;

    /**
     * The content information.
     */
    private Content content = null;

    /**
     * The body string.
     */
    private String body = "";

    public List<BEvent> listEvents = new ArrayList<>();

    private CookieStore cookieStore;
    
    public RESTRequest() {
        cookieStore = new BasicCookieStore();
        
    }
    
    public CookieStore getCookieStore() {
        return cookieStore;
    }
    public void setCookieStore( CookieStore cookieStore) {
        this.cookieStore = cookieStore;
    }
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

    public void setHeaderUrl( String headerUrl ) {
        this.headerUrl = headerUrl;
    }
    public String getHeaderUrl() {
        return headerUrl;
    }
    public void setUri(String uri ) {
        this.uri = uri;
    }
    public String getUri() {
        return uri;
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
    /**
     * Ignore value setter.
     * 
     * @param ignore The ignore new value.
     */
    public void setIgnore(final boolean ignore) {
        this.ignore = ignore;
    }

    /**
     * Result of the REST CALL is send to the CollectOutput
     * @param collectOutput
     */
    public void setCollectOutput(CollectOutput collectOutput) {
        this.collectOutput = collectOutput;
    }
    public CollectOutput getCollectOutput() {
        if (collectOutput==null) {
            collectOutput = new CollectOutput();
            collectOutput.setPolicy(POLICYOUTPUT.STRING);
        }
        return this.collectOutput;
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
      
    }
    public void setHeader(final String key, final String value) {
        // final Header header = new BasicHeader(key, value);
        for (int i=0;i<listHeaders.size();i++)
           if (listHeaders.get(i).getName().equals(key)) {
               listHeaders.set(i, new BasicHeader(key, value));
               return;
           }
        listHeaders.add( new BasicHeader(key, value) );
     }
    
    public void addHeader( Header header ) {
        listHeaders.add( header );
    }
    
    public boolean existHeader( String name ) {
        for (Header header : listHeaders)
            if (header.getName().equals(name))
                return true;
       return false;
    }
    /**
     * Remove a header
     * @param name
     */
    public void removeHeader( String name ) {
        // attention, a header may be multiple
        List<Header> newList = new ArrayList<>();
        for (Header header : listHeaders)
            if (! header.getName().equals(name))
                newList.add( header );
       listHeaders = newList;
    }

    public void addHeaders( List<Header> listHeader, boolean doNotInsertIfAlreadyPresent ) {
        if (doNotInsertIfAlreadyPresent) {
            List<Header> accumulateHeader = new ArrayList<>();
            for(Header header : listHeader) {
                if (! existHeader( header.getName()))
                    accumulateHeader.add( header );
            }
            this.listHeaders.addAll( accumulateHeader );
        } else 
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
