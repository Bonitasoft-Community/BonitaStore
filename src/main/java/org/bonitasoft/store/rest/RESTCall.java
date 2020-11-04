package org.bonitasoft.store.rest;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.AuthSchemeBase;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;


public class RESTCall {

    static Logger logger = Logger.getLogger(RESTCall.class.getName());
    
    private static final String HTTP_PROTOCOL = "HTTP";
    private static final int HTTP_PROTOCOL_VERSION_MAJOR = 1;
    private static final int HTTP_PROTOCOL_VERSION_MINOR = 1;
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * @param request
     * @return
     */

    public static RESTResponse executeWithRedirect(final RESTRequest restRequest, int connectionTimeout) throws Exception {
        int redirectCount=0;
        RESTResponse response=null;
        String uriRedirect=null;
        List<String> historyCall = new ArrayList<>();
        do
        {
            response = execute(restRequest, connectionTimeout);
            uriRedirect =  response.getUrlRedirect();
            historyCall.addAll(response.getHistoryCall());
            
            if (uriRedirect!=null)
            {
                if (uriRedirect.startsWith("http"))
                    restRequest.setUrl( new URL( uriRedirect));
                else 
                {
                    restRequest.setUri( uriRedirect );
                    restRequest.calculateUrlFromUri();
                }
            }
            redirectCount++;
        
        } while( uriRedirect!=null || redirectCount>5);
        
        // override the history call
        response.setHistoryCall( historyCall );
        return response;
    }
    
    /**
     * @param request
     * @return
     */

    public static RESTResponse execute(final RESTRequest restRequest, int connectionTimeout) throws Exception {
        CloseableHttpClient httpClient = null;

        try {
            final URL url = restRequest.getUrl();
            StringBuilder logRestHttp = new StringBuilder();
            
            
            
            final RequestBuilder requestBuilder = getRequestBuilderFromMethod(restRequest.getRestMethod());
            logRestHttp.append("["+restRequest.getRestMethod()+"]");
            final String urlStr = url.toString();
            requestBuilder.setUri(urlStr);
            
            logRestHttp.append(urlStr);
            logRestHttp.append("connectionTimeout["+connectionTimeout+"] isRedirect["+restRequest.isRedirect()+"]");
            
            final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
            httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false));

            
            requestBuilder.setVersion(new ProtocolVersion(HTTP_PROTOCOL, HTTP_PROTOCOL_VERSION_MAJOR, HTTP_PROTOCOL_VERSION_MINOR));            
            
            logRestHttp.append(" Headers: [");
            for (Header header : restRequest.getHeaders()) {
                requestBuilder.addHeader(header);
                logRestHttp.append(header.getName()+":"+header.getValue()+"; ");
            }
            logRestHttp.append("]");

            if (!RESTHTTPMethod.GET.equals(RESTHTTPMethod.valueOf(requestBuilder.getMethod()))) {
                final String body = restRequest.getBody();
                if (body != null) {
                    requestBuilder.setEntity(
                            new StringEntity(restRequest.getBody(),
                                    ContentType.create(restRequest.getContent().getContentType(),
                                            restRequest.getContent().getCharset( RESTCharsets.UTF_8).getValue())));
                }
                logRestHttp.append("Body["+restRequest.getBody()+"] ContentType["+restRequest.getContent().getContentType()+"]");
            }
            // request Config
            final Builder requestConfigurationBuilder = RequestConfig.custom();
            requestConfigurationBuilder.setConnectionRequestTimeout(connectionTimeout);
            requestConfigurationBuilder.setRedirectsEnabled(restRequest.isRedirect());
            final RequestConfig requestConfig = requestConfigurationBuilder.build();

            requestBuilder.setConfig(requestConfig);

            final HttpContext httpContext = getHttpContext(
                    requestConfigurationBuilder,
                    restRequest.getAuthorization(),
                    httpClientBuilder,
                    requestBuilder,
                    restRequest);

            final HttpUriRequest httpRequest = requestBuilder.build();


            httpClient = httpClientBuilder.build();

            logger.info("Call "+logRestHttp);
            // ---------------------------------------------------- Execution now
            long cumulTime = 0;
            final long startTime = System.currentTimeMillis();
            final HttpResponse httpResponse = httpClient.execute(httpRequest, httpContext);
            final long endTime = System.currentTimeMillis();
            cumulTime += endTime - startTime;
            // --------------------------------------------------- answer
            final RESTResponse response = new RESTResponse();
            StringBuffer logRestResponse = new StringBuffer();

            response.setStatusCode(httpResponse.getStatusLine().getStatusCode());
            logRestResponse.append("Status:"+httpResponse.getStatusLine().getStatusCode());

            response.setExecutionTime(cumulTime);
            response.setMessage(httpResponse.getStatusLine().toString());

            final Header[] responseHeaders = httpResponse.getAllHeaders();
            logRestResponse.append(" Headers[");
            for (final Header header : responseHeaders) {
                response.addHeader(header);
                logRestResponse.append(header.getName()+":"+header.getValue()+";");
            }
            logRestResponse.append("]");
            
            // cookie store may be updated by the http call
            response.setCookieStore( restRequest.getCookieStore() );
            
            CollectOutput collectOutput = restRequest.getCollectOutput();
            if (httpResponse.getStatusLine().getStatusCode()!= 301 && httpResponse.getStatusLine().getStatusCode()!=302) {
                collectOutput.collectHttpResponse( httpResponse );                
            }
            
            logger.info("Response "+logRestResponse + collectOutput.trace());
            
            response.addHistoryCall(httpResponse.getStatusLine().getStatusCode()+" "+logRestHttp.toString()+" Response "+logRestResponse.toString());
            return response;
        } catch (final Exception ex) {
            throw ex;
        } finally {
            try {
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (final IOException ex) {

            }
        }

    }

    private static RequestBuilder getRequestBuilderFromMethod(final RESTHTTPMethod method) {
        switch (method) {
            case GET:
                return RequestBuilder.get();
            case POST:
                return RequestBuilder.post();
            case PUT:
                return RequestBuilder.put();
            case DELETE:
                return RequestBuilder.delete();
            default:
                throw new IllegalStateException("Impossible to get the RequestBuilder from the \"" + method.name() + "\" name.");
        }
    }

    /**
     * get the HTTP Context
     * @param requestConfigurationBuilder
     * @param authorization
     * @param urlHost
     * @param urlPort
     * @param urlProtocol
     * @param httpClientBuilder
     * @param requestBuilder
     * @param request
     * @return
     */
    private static HttpClientContext getHttpContext(
            final Builder requestConfigurationBuilder,
            final Authorization authorization,
            final HttpClientBuilder httpClientBuilder,
            final RequestBuilder requestBuilder,
            RESTRequest request) {
        
        final URL url = request.getUrl();
        final String urlHost = url.getHost();
        int urlPort = url.getPort();
        if (url.getPort() == -1) {
            urlPort = url.getDefaultPort();
        }
        final String urlProtocol = url.getProtocol();

        
        HttpClientContext httpContext = null;
        
        if (authorization != null) {
            if (authorization instanceof BasicDigestAuthorization) {
                final List<String> authPrefs = new ArrayList<>();
                if (((BasicDigestAuthorization) authorization).isBasic()) {
                    authPrefs.add(AuthSchemes.BASIC);
                } else {
                    authPrefs.add(AuthSchemes.DIGEST);
                }
                requestConfigurationBuilder.setTargetPreferredAuthSchemes(authPrefs);
                final BasicDigestAuthorization castAuthorization = (BasicDigestAuthorization) authorization;

                final String username = castAuthorization.getUsername();
                final String password = castAuthorization.getPassword();
                String host = castAuthorization.getHost();
                if (castAuthorization.getHost() != null && castAuthorization.getHost().isEmpty()) {
                    host = urlHost;
                }
                String realm = castAuthorization.getRealm();
                if (castAuthorization.getRealm() != null && castAuthorization.getRealm().isEmpty()) {
                    realm = AuthScope.ANY_REALM;
                }

                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(host, urlPort, realm),
                        new UsernamePasswordCredentials(username, password));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);

                if (castAuthorization.isPreemptive()) {
                    final AuthCache authoriationCache = new BasicAuthCache();
                    AuthSchemeBase authorizationScheme = new DigestScheme();
                    if (castAuthorization instanceof BasicDigestAuthorization) {
                        authorizationScheme = new BasicScheme();
                    }
                    authoriationCache.put(new HttpHost(urlHost, urlPort, urlProtocol), authorizationScheme);
                    final HttpClientContext localContext = HttpClientContext.create();
                    localContext.setAuthCache(authoriationCache);
                    httpContext = localContext;
                }
            } else if (authorization instanceof NtlmAuthorization) {
                final List<String> authPrefs = new ArrayList<String>();
                authPrefs.add(AuthSchemes.NTLM);
                requestConfigurationBuilder.setTargetPreferredAuthSchemes(authPrefs);

                final NtlmAuthorization castAuthorization = (NtlmAuthorization) authorization;
                final String username = castAuthorization.getUsername();
                final String password = castAuthorization.getPassword();

                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        AuthScope.ANY,
                        new NTCredentials(username, password, castAuthorization.getWorkstation(), castAuthorization.getDomain()));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            } else if (authorization instanceof HeaderAuthorization) {
                final HeaderAuthorization castAuthorization = (HeaderAuthorization) authorization;
                final String authorizationHeader = castAuthorization.getValue();
                if (authorizationHeader != null && !authorizationHeader.isEmpty()) {
                    final Header header = new BasicHeader(AUTHORIZATION_HEADER, authorizationHeader);
                    requestBuilder.addHeader(header);
                }
            }
        }
        if (httpContext == null) {
            httpContext = new HttpClientContext();
        }
        httpContext.setCookieStore(request.getCookieStore());

        return httpContext;
    }

   
    
   
    
}
