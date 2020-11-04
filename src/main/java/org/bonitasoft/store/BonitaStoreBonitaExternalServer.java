package org.bonitasoft.store;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.client.CookieStore;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.Artifact.TypeArtifact;
import org.bonitasoft.store.artifact.FactoryArtifact;
import org.bonitasoft.store.artifact.FactoryArtifact.ArtifactResult;
import org.bonitasoft.store.rest.CollectOutput;
import org.bonitasoft.store.rest.CollectOutput.POLICYOUTPUT;
import org.bonitasoft.store.rest.Content;
import org.bonitasoft.store.rest.RESTCall;
import org.bonitasoft.store.rest.RESTCharsets;
import org.bonitasoft.store.rest.RESTHTTPMethod;
import org.bonitasoft.store.rest.RESTRequest;
import org.bonitasoft.store.rest.RESTResponse;
import org.bonitasoft.store.toolbox.LoggerStore;
import org.bonitasoft.store.toolbox.TypesCast;

/**
 * Access an external Bonita server
 * There is two type of access:
 * - via the JAVA API which is the main part to get all artifacts
 * - via the REST API, which is the only way to call an Custom Page
 * 
 *  ATTENTION: Theses method are thread safe, but keep in mind the bonitaServer keep in memory the connection information 
 *    BUT a BonitaServer does not allow to have 2 clients connected at the same time.
 *    So, if 2 BonitaStoreBonitaExternalServer is created, the second object is not connected, so it will connect. The Bonita Server will then erase the first connection.
 *    The first object will have a "not connected". 
 * ==> All threads should use the same BonitaStoreBonitaExternalServer

 */
public class BonitaStoreBonitaExternalServer extends BonitaStore {

    private static BEvent eventBadUrl = new BEvent(BonitaStoreBonitaExternalServer.class.getName(), 1,
            BEvent.Level.APPLICATIONERROR, "Bad Parameters",
            "The URL given is not correct, it's malformed", "Url can't be call", "Check the URL");

    private static BEvent eventConnectionError = new BEvent(BonitaStoreBonitaExternalServer.class.getName(), 2,
            BEvent.Level.APPLICATIONERROR, "Connection error",
            "Connection is impossible (bad parameters, or server is not started", "Connection impossible", "Check the Bonita External server");

    private static BEvent eventErrorRestCall = new BEvent(BonitaStoreBonitaExternalServer.class.getName(), 3,
            BEvent.Level.APPLICATIONERROR, "Rest Call Error",
            "The REST Call failed", "Communication failed", "Check the Bonita External server");

    private static BEvent eventErrorJavaCall = new BEvent(BonitaStoreBonitaExternalServer.class.getName(), 4,
            BEvent.Level.APPLICATIONERROR, "Java Call Error",
            "The Java Call failed", "Communication failed", "Check the Bonita External server");
    private static BEvent EVENT_NOT_IMPLEMENTED = new BEvent(BonitaStoreBonitaExternalServer.class.getName(), 4, Level.APPLICATIONERROR, "Not yet implemented", "The function is not yet implemented", "No valid return", "Wait the implementation");

    private int connectionTimeout = 60000; // 1 mn

    // RESTRequest restRequest;

    private String protocol; // "http" 
    private String server; // "localhost" 
    private int port; //8080
    private String applicationName; // "bonita"
    private String userName;
    private String password;
    private int scopeInHour = 72 * 24;

    protected BonitaStoreBonitaExternalServer(String protocole, String server, int port, String applicationName, String userName, String password) {
        this.protocol = protocole;
        this.server = server;
        this.port = port;
        this.applicationName = applicationName;
        this.userName = userName;
        this.password = password;

    }

    @Override
    public String getName() {
        return "BonitaExternalServer";
    }

    @Override
    public String getExplanation() {
        return "Give information to connect an External Bonita Server. All artifacts deployed on this server will be study to be deployed on this server.";
    }

    @Override
    public String getId() {
        return "BonitaExternalServer";
    }

    /** the store can't count the number of download */
    @Override
    public boolean isManageDownload() {
        return false;
    }

    private final static String CST_TYPE_BONITASERVER = "BonitaServer";

    @Override
    public String getType() {
        return CST_TYPE_BONITASERVER;
    }

    public int getScopeInHour() {
        return scopeInHour;
    };

    public void setScopeInHour(int scopeInHour) {
        this.scopeInHour = scopeInHour;
    }

    @Override
    public void fullfillMap(Map<String, Object> map) {
        map.put("protocole", protocol);
        map.put("host", server);
        map.put("port", port);
        map.put("applicationName", applicationName);
        map.put("login", userName);
        map.put("password", password);
        map.put("scopeinhour", scopeInHour);
    }

    /**
     * @param source
     * @return
     */
    public static BonitaStore getInstancefromMap(Map<String, Object> source) {
        try {
            String type = (String) source.get(CST_BONITA_STORE_TYPE);
            if (!CST_TYPE_BONITASERVER.equals(type))
                return null;

            String protocole = TypesCast.getString(source.get("protocole"), "http");
            String server = TypesCast.getString(source.get("host"), null);
            int port = TypesCast.getInteger(source.get("port"), 8080);
            String applicationName = TypesCast.getString(source.get("applicationName"), "bonita");
            String userName = TypesCast.getString(source.get("login"), null);
            String password = TypesCast.getString(source.get("password"), null);
            int scopeInHour = TypesCast.getInteger(source.get("scopeinhour"), 24 * 7);
            BonitaStoreFactory storeFactory = BonitaStoreFactory.getInstance();
            BonitaStoreBonitaExternalServer store = storeFactory.getInstanceBonitaExternalServer(protocole, server, port, applicationName, userName, password,true);
            store.setScopeInHour(scopeInHour);
            store.setDisplayName((String) source.get(CST_BONITA_STORE_DISPLAYNAME));
            return store;
        } catch (Exception e) {
            return null;
        }

    }

    @Override
    public BonitaStoreResult getListArtifacts(BonitaStoreParameters detectionParameters, LoggerStore loggerStore) {
        BonitaStoreResult storeResult = new BonitaStoreResult("getListContent");
        FactoryArtifact factoryArtifact = FactoryArtifact.getInstance();
        try {

            // get list process
            // get list ressource
            // API/portal/page?p=0&c=1000&o=lastUpdateDate%20DESC&f=processDefinitionId%3d&f=isHidden%3dfalse&f=contentType%3dpage
            String uri = "API/portal/page?p=0&c=1000";
            CollectOutput collectOutput = new CollectOutput();
            collectOutput.setPolicy(POLICYOUTPUT.JSON);
            
            RestApiResult restApiResult = callRestJson(uri.toString(), "GET", "", "application/json;charset=UTF-8", RESTCharsets.UTF_8.getValue(), collectOutput);
            restApiResult.jsonResult = collectOutput.getJson();

            if (restApiResult.httpStatus != 200) { // replace the event 
                storeResult.addEvent(eventConnectionError);
            } else {
                storeResult.addEvents(restApiResult.listEvents);
            }
            if (BEventFactory.isError(storeResult.getEvents()))
                return storeResult;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> listResources = (List<Map<String, Object>>) restApiResult.jsonResult;
            for (Map<String, Object> mapResource : listResources) {
                if (Boolean.TRUE.equals(TypesCast.getBoolean(mapResource.get("isProvided"), false)))
                    continue;
                TypeArtifact type = null;
                String name ="urlToken";
                // the information behind page.getName() is the displayName in the REST Result !
                
                //              contentName                         displayName          urlToken                   page
                // page         "CustomPageForklift-1.5.0.zip"      "ForkLift"           "custompage_forklift"      "custompage_ForkLift"
                // RestApi      "custompage_contextaccess"          "Rest Context"       "custompage_ContextAccess" "custompage_contextaccess"
                // Layout:      "layout-MyLayoutBlue.zip"           MyLayoutBlue         "custompage_MyLayoutBlue"
                // theme:       contentName: "bonita-redtheme.zip"  "My Red theme"       "custompage_redthem"      "custompage_redtheme"
                if ("page".equalsIgnoreCase(TypesCast.getString(mapResource.get("contentType"), ""))) {
                    type = TypeArtifact.CUSTOMPAGE;
                }
                
                if ("layout".equalsIgnoreCase(TypesCast.getString(mapResource.get("contentType"), ""))) {
                    type = TypeArtifact.LAYOUT;
                }

                if ("theme".equalsIgnoreCase(TypesCast.getString(mapResource.get("contentType"), ""))) {
                    type = TypeArtifact.THEME;
                }

                if ("form".equalsIgnoreCase(TypesCast.getString(mapResource.get("contentType"), ""))) {
                    type = TypeArtifact.CUSTOMPAGE;
                }

                if ("apiExtension".equalsIgnoreCase(TypesCast.getString(mapResource.get("contentType"), ""))) {
                    type = TypeArtifact.RESTAPI;
                }

                if (type != null) {
                    ArtifactResult artifactResult = new ArtifactResult();
                    artifactResult.artifact = factoryArtifact.getFromType(type,
                            TypesCast.getString(mapResource.get(name), ""), 
                            null,
                            TypesCast.getString(mapResource.get("description"), ""),
                            TypesCast.getDateBonitaRest(mapResource.get("creationDate"), null),
                            TypesCast.getDateBonitaRest(mapResource.get("lastUpdateDate"), null),
                            this);
                     
                    storeResult.addDetectedArtifact(detectionParameters, artifactResult);
                }
            }

        } catch (Exception e) {
            storeResult.addEvent(new BEvent(eventErrorJavaCall, e, "Call server[" + server + "] port[" + port + "]"));
        }

        return storeResult;
    }

    @Override
    public BonitaStoreResult loadArtifact(final Artifact artifact, UrlToDownload urlToDownload, final LoggerStore logBox) {
        BonitaStoreResult storeResult = new BonitaStoreResult("downloadContent");
        storeResult.addEvent(EVENT_NOT_IMPLEMENTED);

        return storeResult;
    }

    @Override
    public BonitaStoreResult ping(LoggerStore logBox) {
        BonitaStoreResult storeResult = new BonitaStoreResult("downloadContent");
        storeResult.addEvents(connectViaJava());

        return storeResult;
    }

    public String getUrlDescription() {
        String urlString = protocol + "://" + server + ":" + port + "/" + applicationName;

        return urlString + " User[" + userName + "]";

    }
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* RestCall */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    public class RestApiResult {

        public List<BEvent> listEvents = new ArrayList<>();
        public Object jsonResult;
        public long httpStatus;
        public long executionTime;
        public RESTResponse restResponse;
        public List<String> historyCall = new ArrayList<>();

    }

       /**
     * Call A Bonita server.
     * This method is thread safe, but keep in mind the bonitaServer keep in memory the connection information AND the Bonitaserver does not allow to have 2 clients connected at the same time.
     * The second connection will clear the first connection 
     * @param uri
     * @param method
     * @param body
     * @param contentType
     * @param charset
     * @param isBinaryOutput
     * @return
     */
    // bob private RESTRequest restRequestObject = null;
    public RestApiResult callRestJson(String uri, String method, String body, String contentType, String charset, CollectOutput collectOutput) {
        RestApiResult restApiResult = new RestApiResult();
        long timeBegin = System.currentTimeMillis();
        // create a new REST REQUEST
        // restRequestObject = new RESTRequest();
        if (! isConnected) {
            ConnectResponse connectedResponse = connectViaRestAPI();
            restApiResult.listEvents.addAll(connectedResponse.listEvents);
            if (connectedResponse.restResponse !=null)
                restApiResult.historyCall.addAll( connectedResponse.restResponse.getHistoryCall());
            if (BEventFactory.isError(restApiResult.listEvents))
                return restApiResult;
        }

        try {
            // test a simple call first
            // RESTResponse restResponseUnused = callRest("API/system/session/unusedId", "GET", "", "application/json;charset=UTF-8", charset);
            // restApiResult.jsonResult = JSONValue.parse(restResponseUnused.getBody());

            // do the call now

            restApiResult.restResponse = callRest(uri, method, body, contentType, charset, collectOutput);
            restApiResult.historyCall.addAll(  restApiResult.restResponse.getHistoryCall());
            
            restApiResult.jsonResult = collectOutput.getJson();
            restApiResult.httpStatus = restApiResult.restResponse.getStatusCode();
            // include the Connection and the result
            long timeEnd = System.currentTimeMillis();
            restApiResult.executionTime = timeEnd - timeBegin;

            return restApiResult;
        } catch (Exception e) {
            restApiResult.listEvents.add(new BEvent(eventErrorRestCall, e, "During call " + uri));
        }
        long timeEnd = System.currentTimeMillis();
        restApiResult.executionTime = timeEnd - timeBegin;
        return restApiResult;
    }

    /**
     * get a file
     * @param uri
     * @param method
     * @param body
     * @param contentType
     * @param charset
     * @return
     */
    public RestApiResult callRestFile(String uri, String method, String body, String contentType, String charset) {
        RestApiResult restApiResult = new RestApiResult();
        long timeBegin = System.currentTimeMillis();
        // create a new REST REQUEST
        // restRequest = new RESTRequest();
        if (! isConnected) {
            ConnectResponse connectedResponse = connectViaRestAPI();
            restApiResult.listEvents.addAll(connectedResponse.listEvents);
            if (connectedResponse.restResponse !=null)
                restApiResult.historyCall.addAll( connectedResponse.restResponse.getHistoryCall());
            if (BEventFactory.isError(restApiResult.listEvents))
                return restApiResult;
    }
    
        try {
            // test a simple call first
            // RESTResponse restResponseUnused = callRest("API/system/session/unusedId", "GET", "", "application/json;charset=UTF-8", charset);
            // restApiResult.jsonResult = JSONValue.parse(restResponseUnused.getBody());

            // do the call now
            CollectOutput collectOutput = new CollectOutput();
            collectOutput.setPolicy(POLICYOUTPUT.BYTEARRAY);
            // what do we do for the result ? 
            restApiResult.restResponse = callRest(uri, method, body, contentType, charset, collectOutput);
            restApiResult.historyCall.addAll(  restApiResult.restResponse.getHistoryCall());
            
            
            restApiResult.httpStatus = restApiResult.restResponse.getStatusCode();
            // include the Connection and the result
            long timeEnd = System.currentTimeMillis();
            restApiResult.executionTime = timeEnd - timeBegin;

            return restApiResult;
        } catch (Exception e) {
            restApiResult.listEvents.add(new BEvent(eventErrorRestCall, e, "During call " + uri));
        }
        long timeEnd = System.currentTimeMillis();
        restApiResult.executionTime = timeEnd - timeBegin;
        return restApiResult;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Connection */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    APISession apiSession = null;
    LoginAPI loginAPI = null;

    private List<BEvent> connectViaJava() {
        List<BEvent> listEvents = new ArrayList<>();
        try {
            final Map<String, String> map = new HashMap<>();
            map.put("server.url", protocol + "://" + server + ":" + port); //  "http://localhost:8080"
            map.put("application.name", applicationName); // "bonita"
            APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, map);

            // Set the username and password
            // get the LoginAPI using the TenantAPIAccessor
            loginAPI = TenantAPIAccessor.getLoginAPI();
            // log in to the tenant to create a session
            apiSession = loginAPI.login(userName, password);
            // get the identityAPI bound to the session created previously.

        } catch (Exception e) {
            listEvents.add(new BEvent(eventConnectionError, e, "Connect to [" + protocol + "://" + server + ":" + port + "] Application[" + applicationName + "]"));
        }
        return listEvents;

    }


    private boolean isConnected=false;
    public List<Header> listHeaderRest = new ArrayList<>();;
    public CookieStore cookieStore = null; 
    private class ConnectResponse {
        List<BEvent> listEvents = new ArrayList<>();
        RESTResponse restResponse;
    }
    /**
     * Connect to the server. Maybe already connected by a different thread.
     */
    private synchronized ConnectResponse connectViaRestAPI() {
        
        ConnectResponse connectResponse = new ConnectResponse();
        // maybe already connected by a second thread?
        if (isConnected)
            return connectResponse;
        try {
            connectResponse.restResponse = callRest("loginservice?", "POST", "username=" + userName + "&password=" + password + "&redirect=false", "application/x-www-form-urlencoded", RESTCharsets.UTF_8.getValue(), CollectOutput.getInstanceString());
            if (connectResponse.restResponse.getStatusCode() != 200) {
                connectResponse.listEvents.add(new BEvent(eventConnectionError, "Call to [" + protocol + "://" + server + ":" + port + "] Application[" + applicationName + "] Username["+userName+"]"));
            }
            listHeaderRest = connectResponse.restResponse.getHeaders();
            cookieStore = connectResponse.restResponse.getCookieStore();
            isConnected=true;
        } catch (BadUrlException e) {
            connectResponse.listEvents.add(new BEvent(eventBadUrl, e.url));
        } catch (Exception e) {
            connectResponse.listEvents.add(new BEvent(eventConnectionError, e, "Call to [" + protocol + "://" + server + ":" + port + "] Application[" + applicationName + "]"));
        }

        return connectResponse;
    }
    /**
     * remove the current connection flags 
     */
    public void resetConnection( ) {
        isConnected=false;
        listHeaderRest.clear();
        cookieStore=null;
    }

    
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* CallRest */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    /**
     * @param uri
     * @param method
     * @param body
     * @param contentType
     * @param headerList
     * @return
     * @throws Exception
     */
    private RESTResponse callRest(String uri, String method, String body, String contentType, String charset, CollectOutput collectOutput) throws Exception {
        // the RESTRequest restRequest must be created first. it contains all cookies
        RESTRequest restRequest = new RESTRequest();
        restRequest.setCollectOutput(collectOutput);
        restRequest.setHeaderUrl( protocol + "://" + server + ":" + port + "/");
        // application is based in the URI. In case of a redirect, we just need to call the same server, so end at the port number. The Redirect will contains the application name inside.
        restRequest.setUri( applicationName + "/" + uri);
        try {
            restRequest.calculateUrlFromUri();
        } catch (final MalformedURLException e) {
            throw new BadUrlException(restRequest.getHeaderUrl() + restRequest.getUri());
        }

        final Content content = new Content();
        content.setContentType(contentType);
        if (charset != null) {
            content.setCharset(RESTCharsets.getRESTCharsetsFromValue(charset));
        }
        restRequest.setContent(content);
        restRequest.addHeader("Content-type", contentType);
        
        restRequest.setBody(body);
        restRequest.setRestMethod(RESTHTTPMethod.getRESTHTTPMethodFromValue(method));
        //request.setRedirect(true);
        //request.setIgnore(false);
        if (!listHeaderRest.isEmpty())
            restRequest.addHeaders(listHeaderRest, true);
        if (cookieStore !=null)
            restRequest.setCookieStore(cookieStore);
        return RESTCall.executeWithRedirect(restRequest, connectionTimeout);

    }

    private class BadUrlException extends Exception {

        private static final long serialVersionUID = 1L;
        public String url;

        BadUrlException(String url) {
            this.url = url;
        }

    }
}
