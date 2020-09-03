package org.bonitasoft.store.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
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
import org.apache.http.client.protocol.ClientContext;
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
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.bonitasoft.store.BonitaStore.UrlToDownload;
import org.bonitasoft.store.source.git.RESTResultKeyValueMap;

public class RESTCall {

    private static final String HTTP_PROTOCOL = "HTTP";
    private static final int HTTP_PROTOCOL_VERSION_MAJOR = 1;
    private static final int HTTP_PROTOCOL_VERSION_MINOR = 1;
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * @param request
     * @return
     */

    public static RESTResponse executeWithRedirect(final RESTRequest request, int connectionTimeout) throws Exception {
        int redirectCount=0;
        RESTResponse response=null;
        String uriRedirect=null;
        do
        {
            response = execute(request, connectionTimeout);
            uriRedirect =  response.getUrlRedirect();
            if (uriRedirect!=null)
            {
                if (uriRedirect.startsWith("http"))
                    request.setUrl( new URL( uriRedirect));
                else 
                {
                    request.uri = uriRedirect;
                    request.calculateUrlFromUri();
                }
            }
            redirectCount++;
        
        } while( uriRedirect!=null || redirectCount>5);
        return response;
    }
    
    /**
     * @param request
     * @return
     */

    public static RESTResponse execute(final RESTRequest request, int connectionTimeout) throws Exception {
        CloseableHttpClient httpClient = null;

        try {
            final URL url = request.getUrl();

            final Builder requestConfigurationBuilder = RequestConfig.custom();
            requestConfigurationBuilder.setConnectionRequestTimeout(connectionTimeout);
            requestConfigurationBuilder.setRedirectsEnabled(request.isRedirect());
            final RequestConfig requestConfig = requestConfigurationBuilder.build();

            final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
            httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false));

            final RequestBuilder requestBuilder = getRequestBuilderFromMethod(request.getRestMethod());
            requestBuilder.setVersion(new ProtocolVersion(HTTP_PROTOCOL, HTTP_PROTOCOL_VERSION_MAJOR, HTTP_PROTOCOL_VERSION_MINOR));            
            final String urlStr = url.toString();
            requestBuilder.setUri(urlStr);
            for (Header header : request.getHeaders()) {
                requestBuilder.addHeader(header);
            }
            if (!RESTHTTPMethod.GET.equals(RESTHTTPMethod.valueOf(requestBuilder.getMethod()))) {
                final String body = request.getBody();
                if (body != null) {
                    requestBuilder.setEntity(
                            new StringEntity(request.getBody(),
                                    ContentType.create(request.getContent().getContentType(),
                                            request.getContent().getCharset( RESTCharsets.UTF_8).getValue())));
                }
            }

            requestBuilder.setConfig(requestConfig);

            final HttpContext httpContext = getHttpContext(
                    requestConfigurationBuilder,
                    request.getAuthorization(),
                    httpClientBuilder,
                    requestBuilder,
                    request);

            final HttpUriRequest httpRequest = requestBuilder.build();


            httpClient = httpClientBuilder.build();

            // ---------------------------------------------------- Execution now
            long cumulTime = 0;
            final long startTime = System.currentTimeMillis();
            final HttpResponse httpResponse = httpClient.execute(httpRequest, httpContext);
            final long endTime = System.currentTimeMillis();
            cumulTime += endTime - startTime;
            // --------------------------------------------------- answer
            final Header[] responseHeaders = httpResponse.getAllHeaders();

            final RESTResponse response = new RESTResponse();
            response.setExecutionTime(cumulTime);
            response.setStatusCode(httpResponse.getStatusLine().getStatusCode());
            response.setMessage(httpResponse.getStatusLine().toString());

            for (final Header header : responseHeaders) {
                response.addHeader(header);
            }

            final HttpEntity entity = httpResponse.getEntity();
            if (entity != null) {
                if (request.isIgnore()) {
                    EntityUtils.consumeQuietly(entity);
                } else if (request.isStringOutput()) {
                    final InputStream inputStream = entity.getContent();

                    final StringWriter stringWriter = new StringWriter();
                    IOUtils.copy(inputStream, stringWriter);
                    if (stringWriter.toString() != null) {
                        response.setBody(stringWriter.toString());
                    }

                } else {
                    final byte[] contentByte = IOUtils.toByteArray(entity.getContent());

                    response.setContentByte(contentByte);
                }
            }

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
                final String password = new String(castAuthorization.getPassword());

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
