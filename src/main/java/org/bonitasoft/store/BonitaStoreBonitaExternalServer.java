package org.bonitasoft.store;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.PageAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.page.ContentType;
import org.bonitasoft.engine.page.Page;
import org.bonitasoft.engine.page.PageSearchDescriptor;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.store.BonitaStoreBonitaExternalServer.RestApiResult;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.FactoryArtifact;
import org.bonitasoft.store.artifact.Artifact.TypeArtifact;
import org.bonitasoft.store.rest.Content;
import org.bonitasoft.store.rest.RESTCall;
import org.bonitasoft.store.rest.RESTCharsets;
import org.bonitasoft.store.rest.RESTHTTPMethod;
import org.bonitasoft.store.rest.RESTRequest;
import org.bonitasoft.store.rest.RESTResponse;
import org.bonitasoft.store.toolbox.LoggerStore;
import org.bonitasoft.store.toolbox.TypesCast;
import org.json.simple.JSONValue;

import com.sun.xml.bind.v2.util.TypeCast;

/**
 * Access an external Bonita server
 * There is two type of access:
 * - via the JAVA API which is the main part to get all artifacts
 * - via the REST API, which is the only way to call an Custom Page
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

    RESTRequest restRequest;

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
            BonitaStoreBonitaExternalServer store = new BonitaStoreBonitaExternalServer(protocole, server, port, applicationName, userName, password);
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
            RestApiResult restApiResult = callRestJson(uri.toString(), "GET", "", "application/json;charset=UTF-8", RESTCharsets.UTF_8.getValue());
            storeResult.addEvents(restApiResult.listEvents);
            if (restApiResult.httpStatus != 200) {
                storeResult.addEvent(eventConnectionError);
            }
            if (BEventFactory.isError(storeResult.getEvents()))
                return storeResult;
            List<Map<String, Object>> listResources = (List<Map<String, Object>>) restApiResult.jsonResult;
            for (Map<String, Object> mapResource : listResources) {
                Artifact artifact = null;

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

                if (type != null)
                    artifact = factoryArtifact.getFromType(type,
                            TypesCast.getString(mapResource.get(name), ""), 
                            null,
                            TypesCast.getString(mapResource.get("description"), ""),
                            TypesCast.getDateBonitaRest(mapResource.get("creationDate"), null),
                            TypesCast.getDateBonitaRest(mapResource.get("lastUpdateDate"), null),
                            this);

                storeResult.addDetectedArtifact(detectionParameters, artifact);
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
    }

    public RestApiResult callRestJson(String uri, String method, String body, String contentType, String charset) {
        RestApiResult restApiResult = new RestApiResult();
        long timeBegin = System.currentTimeMillis();
        // create a new REST REQUEST
        restRequest = new RESTRequest();
        restApiResult.listEvents.addAll(connectViaRestAPI());
        if (BEventFactory.isError(restApiResult.listEvents))
            return restApiResult;

        try {
            // test a simple call first
            // RESTResponse restResponseUnused = callRest("API/system/session/unusedId", "GET", "", "application/json;charset=UTF-8", charset);
            // restApiResult.jsonResult = JSONValue.parse(restResponseUnused.getBody());

            // do the call now

            restApiResult.restResponse = callRest(uri, method, body, contentType, charset);
            restApiResult.jsonResult = JSONValue.parse(restApiResult.restResponse.getBody());
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

    public RestApiResult callRestFile(String uri, String method, String body, String contentType, String charset) {
        RestApiResult restApiResult = new RestApiResult();
        long timeBegin = System.currentTimeMillis();
        // create a new REST REQUEST
        restRequest = new RESTRequest();
        restApiResult.listEvents.addAll(connectViaRestAPI());
        if (BEventFactory.isError(restApiResult.listEvents))
            return restApiResult;

        try {
            // test a simple call first
            // RESTResponse restResponseUnused = callRest("API/system/session/unusedId", "GET", "", "application/json;charset=UTF-8", charset);
            // restApiResult.jsonResult = JSONValue.parse(restResponseUnused.getBody());

            // do the call now

            restApiResult.restResponse = callRest(uri, method, body, contentType, charset);

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

    public List<Header> listHeaderRest = new ArrayList<>();;

    /**
     * @return
     */
    private List<BEvent> connectViaRestAPI() {
        List<BEvent> listEvents = new ArrayList<>();

        try {
            RESTResponse restResponse = callRest("loginservice?", "POST", "username=" + userName + "&password=" + password + "&redirect=false", "application/x-www-form-urlencoded", RESTCharsets.UTF_8.getValue());
            listHeaderRest = restResponse.getHeaders();
        } catch (BadUrlException e) {
            listEvents.add(new BEvent(eventBadUrl, e.url));
        } catch (Exception e) {
            listEvents.add(new BEvent(eventConnectionError, e, "Call to [" + protocol + "://" + server + ":" + port + "] Application[" + applicationName + "]"));
        }

        return listEvents;
    }

    /**
     * @param uri
     * @param method
     * @param body
     * @param contentType
     * @param headerList
     * @return
     * @throws Exception
     */
    private RESTResponse callRest(String uri, String method, String body, String contentType, String charset) throws Exception {
        // the RESTRequest restRequest must be created first. it contains all cookies

        restRequest.headerUrl = protocol + "://" + server + ":" + port + "/";
        // applicatione is based in the URI. In case of a redirect, we just need to call the same server, so end at the port number. The Redirect will contains the application name inside.
        restRequest.uri = applicationName + "/" + uri;
        try {
            restRequest.calculateUrlFromUri();
        } catch (final MalformedURLException e) {
            throw new BadUrlException(restRequest.headerUrl + restRequest.uri);
        }

        final Content content = new Content();
        content.setContentType(contentType);
        if (charset != null) {
            content.setCharset(RESTCharsets.getRESTCharsetsFromValue(charset));
        }
        restRequest.setContent(content);

        restRequest.setBody(body);
        restRequest.setRestMethod(RESTHTTPMethod.getRESTHTTPMethodFromValue(method));
        //request.setRedirect(true);
        //request.setIgnore(false);
        if (!listHeaderRest.isEmpty())
            restRequest.addHeaders(listHeaderRest);

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
