package org.bonitasoft.store.source.git;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
import org.apache.http.util.EntityUtils;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.store.git.model.Authorization;
import org.bonitasoft.store.git.model.BasicDigestAuthorization;
import org.bonitasoft.store.git.model.Content;
import org.bonitasoft.store.git.model.HeaderAuthorization;
import org.bonitasoft.store.git.model.NtlmAuthorization;
import org.bonitasoft.store.git.model.RESTCharsets;
import org.bonitasoft.store.git.model.RESTHTTPMethod;
import org.bonitasoft.store.git.model.RESTRequest;
import org.bonitasoft.store.git.model.RESTResponse;
import org.bonitasoft.store.toolbox.LoggerStore;
import org.bonitasoft.store.toolbox.LoggerStore.LOGLEVEL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class GithubAccessor {

  final Logger logger = Logger.getLogger(GithubAccessor.class.getName());

  private static final String HTTP_PROTOCOL = "HTTP";
  private static final int HTTP_PROTOCOL_VERSION_MAJOR = 1;
  private static final int HTTP_PROTOCOL_VERSION_MINOR = 1;
  private static final int CONNECTION_TIMEOUT = 60000;
  private static final String AUTHORIZATION_HEADER = "Authorization";

  private final static BEvent eventResultNotExpected = new BEvent(GithubAccessor.class.getName(), 1, Level.APPLICATIONERROR, "Error Github code",
      "The URL call to the github repository does not return a code 200", "Check the URL to github, the login/password");
  private final static BEvent eventNoResult = new BEvent(GithubAccessor.class.getName(), 2, Level.INFO, "No result", "The github repository is empty");

  private static BEvent eventBadUrl = new BEvent(GithubAccessor.class.getName(), 3, BEvent.Level.APPLICATIONERROR, "Bad URL",
      "The URL given is not correct, it's malformed", "Check the URL");

  private static BEvent eventRestRequest = new BEvent(GithubAccessor.class.getName(), 4, BEvent.Level.APPLICATIONERROR, "Can't connect to the GITHUB Server",
      "An error occures when the GITHUB Server is connected", "Check the error");

  private final static BEvent eventBadFormat = new BEvent(GithubAccessor.class.getName(), 5, Level.APPLICATIONERROR, "Bad Format",
      "The Githbub repository is supposed to get back in the content a 'asset' list, which is not found.",
      "The result can't be parse",
      "Check the github repository, you may be no have an access on ?");

  private final static BEvent eventRateLimiteExceeded = new BEvent(GithubAccessor.class.getName(), 6, Level.APPLICATIONERROR, "Rate Limite Exceeded",
      "API rate limit exceeded",
      "The number of search per hour is reach",
      "Wait one hour, or give your login/password github");

  private final static BEvent eventAccess403 = new BEvent(GithubAccessor.class.getName(), 7, Level.APPLICATIONERROR, // level
      "Error code 403", // title
      "Github API return a 403 error", // cause
      "The github order can't be managed", // consequence
      "Check the message"); // action
  /*
   * static public void main(final String[] args) {
   * final ResultLastContrib content = getLastContribReleaseAsset("PierrickVouletBonitasoft",
   * "pwd");
   * }
   */

  private final String mUserName;
  private final String mPassword;
  private final String mUrlRepository;

  public GithubAccessor(final String userName, final String password, final String urlRepository) {
    mUserName = userName;
    mPassword = password;
    mUrlRepository = urlRepository;
  }

  public static class ResultGithub {

    public String content;
    public byte[] contentByte;

    public List<BEvent> listEvents = new ArrayList<BEvent>();

    // the result may be a JSONObject (an hashmap) or a JSONArray (a list) : two type are not compatible...
    public Object jsonResult = null;

    public JSONObject getJsonObject() {
      try {
        return (JSONObject) jsonResult;
      } catch (final Exception e) {
        return null;
      }

    }

    public JSONArray getJsonArray() {
      try {
        return (JSONArray) jsonResult;
      } catch (final Exception e) {
        return null;
      }

    }

    public void checkResultFormat(final boolean formatExpectedIsArray, final String message) {
      if (formatExpectedIsArray && getJsonArray() == null) {
        listEvents.add(new BEvent(eventBadFormat, message));
      }
      if (!formatExpectedIsArray && getJsonObject() == null) {
        listEvents.add(new BEvent(eventBadFormat, message));
      }
    }

    public boolean isError() {
      return BEventFactory.isError(listEvents);
    }
  }

  public String getUrlRepository() {
    return mUrlRepository;
  }

  /**
   * check if the information give for this Github repository are correct
   *
   * @return
   */
  public boolean isCorrect() {
    return mUrlRepository != null && mUrlRepository.trim().length() > 0;
  }

  /**
   * In the github repository, execute a specific Order, in GET method.
   *
   * @return
   */
  @SuppressWarnings("unchecked")
  public ResultGithub executeGetRestOrder(final String order, final String completeOrder, final LoggerStore logBox) {
    //get the latest release
    final ResultGithub resultLastContrib = new ResultGithub();
    final String orderGithub = order == null ? completeOrder : mUrlRepository + order;
    
    final RESTRequest restRequest = buildRestRequest(
        orderGithub,
        "GET",
        "",
        "application/json",
        null, // "UTF-8",
        new ArrayList<ArrayList<String>>(),
        new ArrayList<ArrayList<String>>(),
        mUserName,
        mPassword);
    resultLastContrib.listEvents.addAll(restRequest.listEvents);
    if (BEventFactory.isError(restRequest.listEvents)) {
      return resultLastContrib;
    }
    try {
      final RESTResponse response = execute(restRequest);
      if (logBox.isLog(LOGLEVEL.INFO)) {
        logBox.log(LOGLEVEL.INFO, "GithubAccess.getRestOrder: Url["
            + orderGithub
            + "] Code:"
            + response.getStatusCode()
            + " body:"
            + (response.getBody() == null ? "null" : response.getBody().length() > 50 ? response.getBody().substring(0, 50) + "..." : response
                .getBody()));
      }

      if (response.getStatusCode() == 403) {
        String message = null;
        try {
          resultLastContrib.jsonResult = JSONValue.parse(response.getBody());
          message = (String) ((Map<String, Object>) resultLastContrib.getJsonObject()).get("message");
        } catch (final Exception e) {
        }
        if (message != null && message.startsWith("API rate limit exceeded")) {
          resultLastContrib.listEvents.add(new BEvent(eventRateLimiteExceeded, "Code " + response.getStatusCode() + " on URL ["
              + orderGithub + "]"));
        } else {
          resultLastContrib.listEvents.add(new BEvent(eventAccess403, "Code " + response.getStatusCode() + " Message[" + message + "] on URL ["
              + orderGithub + "]"));
        }

      }

      else if (response.getStatusCode() != 200) {
        resultLastContrib.listEvents.add(new BEvent(eventResultNotExpected, "Code " + response.getStatusCode() + " on URL ["
            + orderGithub + "]"));
        return resultLastContrib;
      }

      resultLastContrib.jsonResult = JSONValue.parse(response.getBody());
    } catch (final Exception e) {
      resultLastContrib.listEvents.add(new BEvent(eventRestRequest, e, "Url[" + mUrlRepository + "] with user[" + mUserName + "]"));
      logBox.logException("Url[" + mUrlRepository + "] with user[" + mUserName + "]", e);
    }
    return resultLastContrib;
  }

  /*
   * final JSONArray assets = (JSONArray) jsonResult.get("assets");
   * String assetURL = null;
   * for(int i = 0; i < assets.size(); i++) {
   * final JSONObject asset = (JSONObject) assets.get(i);
   * if(asset.get("name").toString().startsWith("bonita-internal-contrib-releases-")) {
   * assetURL = asset.get("browser_download_url").toString();
   * break;
   * }
   * }
   * logger.info(assetURL);
   * //-------- get the release asset
   * if(assetURL != null) {
   * final RESTRequest restRequest2 = buildGetLastReleaseRequest(
   * assetURL,
   * "GET",
   * "",
   * "",
   * "UTF-8",
   * new ArrayList<ArrayList<String>>(),
   * new ArrayList<ArrayList<String>>(),
   * username,
   * password);
   * final RESTResponse response2 = execute(restRequest2);
   * // System.out.println(response2.getBody());
   * resultLastContrib.content = response2.getBody();
   * return resultLastContrib;
   * }
   * resultLastContrib.listEvents.add(NoResult);
   * return resultLastContrib;
   * }
   */

  /**
   * @param assetURL
   * @param method
   * @param userName
   * @param password
   * @return
   */
  public ResultGithub getContent(final String assetURL, final String method, final String contentType, final String charSet) {
    final ResultGithub resultLastContrib = new ResultGithub();
    final RESTRequest restRequest = buildRestRequest(
        assetURL,
        "GET",
        "",
        contentType,
        charSet,
        new ArrayList<ArrayList<String>>(),
        new ArrayList<ArrayList<String>>(),
        mUserName,
        mPassword);
    try {
      final RESTResponse response = execute(restRequest);
      // System.out.println(response2.getBody());
      resultLastContrib.content = response.getBody();
    } catch (final Exception e) {
      resultLastContrib.listEvents.add(new BEvent(eventRestRequest, e, "Url[" + mUrlRepository + "] with user[" + mUserName + "]"));

    }
    return resultLastContrib;
  }

  /**
   * return a content, on Binary or on
   * 
   * @param assetURL
   * @param method
   * @param contentType
   * @param charSet
   * @param isBinaryContent
   * @return
   */
  public ResultGithub getBinaryContent(final String assetURL, final String method, final String contentType, final String charSet) {
    final ResultGithub resultLastContrib = new ResultGithub();
    final RESTRequest restRequest = buildRestRequest(
        assetURL,
        "GET",
        "",
        contentType,
        charSet,
        new ArrayList<ArrayList<String>>(),
        new ArrayList<ArrayList<String>>(),
        mUserName,
        mPassword);
    restRequest.setStringOutput(false);
    restRequest.setRedirect(true);
    try {
      final RESTResponse response = execute(restRequest);
      // System.out.println(response.getBody());
      resultLastContrib.contentByte = response.getContentByte();
      resultLastContrib.content = response.getBody();
    } catch (final Exception e) {
      resultLastContrib.listEvents.add(new BEvent(eventRestRequest, e, "Url[" + mUrlRepository + "] with user[" + mUserName + "]"));

    }
    return resultLastContrib;
  }

  /**
   * @param assetURL
   * @param method
   * @param userName
   * @param password
   * @return
   */
  public ResultGithub getBinaryContent2(final String assetURL, final String method, final String contentType, final String charSet) {
    final ResultGithub resultLastContrib = new ResultGithub();

    try {
      final URL url = new URL(assetURL);
      // request.setAuthorization(buildBasicAuthorization(mUserName, mPassword));
      final URLConnection c = url.openConnection();
      c.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; .NET CLR 1.0.3705; .NET CLR 1.1.4322; .NET CLR 1.2.30703)");

      InputStream input;
      input = c.getInputStream();
      final byte[] buffer = new byte[8000];
      int n = -1;

      final OutputStream output = new FileOutputStream(new File("c:/temp/file.zip"));
      while ((n = input.read(buffer)) != -1) {
        if (n > 0) {
          output.write(buffer, 0, n);
        }
      }
      output.close();

    } catch (final Exception e) {
      resultLastContrib.listEvents.add(new BEvent(eventRestRequest, e, "Url[" + mUrlRepository + "] with user[" + mUserName + "]"));

    }
    return resultLastContrib;
  }

  /**
   * @param request
   * @return
   */

  private RESTResponse execute(final RESTRequest request) throws Exception {
    CloseableHttpClient httpClient = null;

    try {
      final URL url = request.getUrl();
      final String urlHost = url.getHost();

      final Builder requestConfigurationBuilder = RequestConfig.custom();
      requestConfigurationBuilder.setConnectionRequestTimeout(CONNECTION_TIMEOUT);
      requestConfigurationBuilder.setRedirectsEnabled(request.isRedirect());
      final RequestConfig requestConfig = requestConfigurationBuilder.build();

      final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
      httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false));

      final RequestBuilder requestBuilder = getRequestBuilderFromMethod(request.getRestMethod());
      requestBuilder.setVersion(new ProtocolVersion(HTTP_PROTOCOL, HTTP_PROTOCOL_VERSION_MAJOR, HTTP_PROTOCOL_VERSION_MINOR));
      int urlPort = url.getPort();
      if (url.getPort() == -1) {
        urlPort = url.getDefaultPort();
      }
      final String urlProtocol = url.getProtocol();
      final String urlStr = url.toString();
      requestBuilder.setUri(urlStr);
      setHeaders(requestBuilder, request.getHeaders());
      if (!RESTHTTPMethod.GET.equals(RESTHTTPMethod.valueOf(requestBuilder.getMethod()))) {
        final String body = request.getBody();
        if (body != null) {
          requestBuilder.setEntity(
              new StringEntity(request.getBody(),
                  ContentType.create(request.getContent().getContentType(),
                      request.getContent().getCharset().getValue())));
        }
      }

      requestBuilder.setConfig(requestConfig);

      final HttpContext httpContext = setAuthorization(
          requestConfigurationBuilder,
          request.getAuthorization(),
          urlHost,
          urlPort,
          urlProtocol,
          httpClientBuilder,
          requestBuilder);

      final HttpUriRequest httpRequest = requestBuilder.build();

      httpClient = httpClientBuilder.build();

      long cumulTime = 0;
      final long startTime = System.currentTimeMillis();
      final HttpResponse httpResponse = httpClient.execute(httpRequest, httpContext);
      final long endTime = System.currentTimeMillis();
      cumulTime += endTime - startTime;

      final Header[] responseHeaders = httpResponse.getAllHeaders();

      final RESTResponse response = new RESTResponse();
      response.setExecutionTime(cumulTime);
      response.setStatusCode(httpResponse.getStatusLine().getStatusCode());
      response.setMessage(httpResponse.getStatusLine().toString());

      for (final Header header : responseHeaders) {
        response.addHeader(header.getName(), header.getValue());
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

  private static HttpContext setAuthorization(
      final Builder requestConfigurationBuilder,
      final Authorization authorization,
      final String urlHost,
      final int urlPort,
      final String urlProtocol,
      final HttpClientBuilder httpClientBuilder,
      final RequestBuilder requestBuilder) {
    HttpContext httpContext = null;
    if (authorization != null) {
      if (authorization instanceof BasicDigestAuthorization) {
        final List<String> authPrefs = new ArrayList<String>();
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

    return httpContext;
  }

  private static void setHeaders(final RequestBuilder requestBuilder, final List<RESTResultKeyValueMap> headerData) {
    for (final RESTResultKeyValueMap aHeaderData : headerData) {
      final String key = aHeaderData.getKey();
      for (final String value : aHeaderData.getValue()) {
        final Header header = new BasicHeader(key, value);
        requestBuilder.addHeader(header);
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
   * @param url
   * @param method
   * @param body
   * @param contentType
   * @param charset
   * @param arrayList
   * @param arrayList2
   * @param username
   * @param password
   * @return
   */
  private RESTRequest buildRestRequest(final String url, final String method, final String body, final String contentType, final String charset,
      final ArrayList<ArrayList<String>> headerList, final ArrayList<ArrayList<String>> cookieList, final String username, final String password) {
    final RESTRequest request = new RESTRequest();
    try {
      request.setUrl(new URL(url));
    } catch (final MalformedURLException e) {
      request.listEvents.add(new BEvent(eventBadUrl, url));
      return request;
    }

    final Content content = new Content();
    if (contentType != null) {
      content.setContentType(contentType);
    }
    if (charset != null) {
      content.setCharset(RESTCharsets.getRESTCharsetsFromValue(charset));
    }
    // request.setContent(content);
    request.setBody(body);
    request.setRestMethod(RESTHTTPMethod.getRESTHTTPMethodFromValue(method));
    //request.setRedirect(true);
    //request.setIgnore(false);
    for (final Object urlheader : headerList) {
      final List<?> urlheaderRow = (List<?>) urlheader;
      request.addHeader(urlheaderRow.get(0).toString(), urlheaderRow.get(1).toString());
    }
    for (final Object urlCookie : cookieList) {
      final List<?> urlCookieRow = (List<?>) urlCookie;
      request.addCookie(urlCookieRow.get(0).toString(), urlCookieRow.get(1).toString());
    }
    if (username != null) {
      request.setAuthorization(buildBasicAuthorization(username, password));
    }
    return request;
  }

  static private BasicDigestAuthorization buildBasicAuthorization(final String username, final String password) {
    final BasicDigestAuthorization authorization = new BasicDigestAuthorization(true);
    authorization.setUsername(username);
    authorization.setPassword(password);

    return authorization;
  }

  /**
   * check if the header contains a redirection
   *
   * @param header
   * @return
   */
  private static String isRedirected(final Header[] headers) {
    String status = null;
    String location = null;
    for (final Header hv : headers) {
      if (hv.getName().equals("Status")) {
        status = hv.getValue();
      }
      if (hv.getName().equals("Location")) {
        location = hv.getValue();
      }
    }
    if (status != null && (status.startsWith("301") || status.startsWith("302"))) {
      return location;
    }
    return null;
  }

  /**
   * return the description for the log
   *
   * @return
   */
  public String toLog() {
    return "url[" + mUrlRepository + "] userName[" + mUserName + "] password[" + (mPassword == null ? "" : "**") + "]";
  }
}
