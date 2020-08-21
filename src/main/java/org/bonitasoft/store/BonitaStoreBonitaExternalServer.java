package org.bonitasoft.store;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStore.UrlToDownload;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.rest.Content;
import org.bonitasoft.store.rest.RESTCall;
import org.bonitasoft.store.rest.RESTCharsets;
import org.bonitasoft.store.rest.RESTHTTPMethod;
import org.bonitasoft.store.rest.RESTRequest;
import org.bonitasoft.store.rest.RESTResponse;
import org.bonitasoft.store.toolbox.LoggerStore;
import org.json.simple.JSONValue;

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
    private static BEvent EVENT_NOT_IMPLEMENTED = new BEvent(BonitaStoreBonitaExternalServer.class.getName(), 4, Level.APPLICATIONERROR, "Not yet implemented", "The function is not yet implemented", "No valid return", "Wait the implementation");

    private int connectionTimeout = 60000; // 1 mn

    private String protocol; // "http" 
    private String server; // "localhost" 
    private int port; //8080
    private String applicationName; // "bonita"
    private String userName;
    private String password;

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
    public String getId() {
        return "BonitaExternalServer";
    }

    /** the store can't count the number of download */
    @Override
    public boolean isManageDownload() {
        return false;
    }

    @Override
    public Map<String, Object> toMap() {
        return new HashMap<>();
    }

    @Override
    public BonitaStoreResult getListArtifacts(DetectionParameters detectionParameters, LoggerStore loggerStore) {
        BonitaStoreResult storeResult = new BonitaStoreResult("getListContent");

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
    }

    public RestApiResult callRestJson(String uri, String method, String body, String contentType, String charset) {
        RestApiResult restApiResult = new RestApiResult();
        restApiResult.listEvents.addAll(connectViaRestAPI());
        if (BEventFactory.isError(restApiResult.listEvents))
            return restApiResult;

        // test a simple call first
        try {
            RESTResponse restResponseUnused = callRest("API/system/session/unusedId", "GET", "", "application/json;charset=UTF-8", charset);
            restApiResult.jsonResult = JSONValue.parse(restResponseUnused.getBody());

            // do the call now

            RESTResponse restResponse = callRest(uri, method, body, contentType, charset);
            restApiResult.jsonResult = JSONValue.parse(restResponse.getBody());
            restApiResult.httpStatus = restResponse.getStatusCode();
            restApiResult.executionTime = restResponse.getExecutionTime();
            return restApiResult;
        } catch (Exception e) {
            restApiResult.listEvents.add(new BEvent(eventErrorRestCall, e, "During call " + uri));
        }
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

    public  List<Header> listHeaderRest = new ArrayList<>();;

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

        RESTRequest restRequest = new RESTRequest();
        restRequest.headerUrl = protocol + "://" + server + ":" + port + "/" + applicationName + "/" ;
        restRequest.uri =  uri;
        try {
            restRequest.calculateUrlFromUri();
        } catch (final MalformedURLException e) {
            throw new BadUrlException(restRequest.headerUrl + restRequest.uri );
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
        if (! listHeaderRest.isEmpty())
            restRequest.addHeaders( listHeaderRest );

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
