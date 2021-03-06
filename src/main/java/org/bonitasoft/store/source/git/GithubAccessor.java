package org.bonitasoft.store.source.git;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.store.rest.BasicDigestAuthorization;
import org.bonitasoft.store.rest.CollectOutput;
import org.bonitasoft.store.rest.CollectOutput.POLICYOUTPUT;
import org.bonitasoft.store.rest.Content;
import org.bonitasoft.store.rest.RESTCall;
import org.bonitasoft.store.rest.RESTCharsets;
import org.bonitasoft.store.rest.RESTHTTPMethod;
import org.bonitasoft.store.rest.RESTRequest;
import org.bonitasoft.store.rest.RESTResponse;
import org.bonitasoft.store.toolbox.LoggerStore;
import org.bonitasoft.store.toolbox.LoggerStore.LOGLEVEL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class GithubAccessor {

    final Logger logger = Logger.getLogger(GithubAccessor.class.getName());

    private static final int CONNECTION_TIMEOUT = 60000;

    private final static BEvent eventResultNotExpected = new BEvent(GithubAccessor.class.getName(), 1, Level.APPLICATIONERROR, "Error Github code",
            "The URL call to the github repository does not return a code 200", "No connection to Github", "Check the URL to github, the login/password");

    private final static BEvent eventNoResult = new BEvent(GithubAccessor.class.getName(), 2, Level.INFO, "No result", "The github repository is empty");

    private static BEvent eventBadUrl = new BEvent(GithubAccessor.class.getName(), 3,
            BEvent.Level.APPLICATIONERROR, "Bad URL",
            "The URL given is not correct, it's malformed", "Url can't be call", "Check the URL");

    private static BEvent eventRestRequest = new BEvent(GithubAccessor.class.getName(), 4, BEvent.Level.APPLICATIONERROR, "Can't connect to the GITHUB Server",
            "An error occures when the GITHUB Server is connected", "No result from Git", "Check the error");

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

    private String mUserName;

    private String mPassword;
    private String mUrlRepository;

    public GithubAccessor(final String userName, final String password, final String urlRepository) {
        mUserName = userName;
        mPassword = password;
        mUrlRepository = urlRepository;
    }
    
    /**
     * Default Header
     */
    private ArrayList<ItemHeader> mDefaultHeaderList = new ArrayList();
    public void addDefaultHeader( ItemHeader itemHeader) {
        mDefaultHeaderList.add( itemHeader);
    }
    public void clearDefaultHeader() {
        mDefaultHeaderList.clear();;
    }
    
    public static class ResultGithub {

        public String content;
        public byte[] contentByte;

        public List<BEvent> listEvents = new ArrayList<>();

        // the result may be a JSONObject (an hashmap) or a JSONArray (a list) : two type are not compatible...
        public Object jsonResult = null;

        public JSONObject getJsonObject() {
            try {
                return (JSONObject) jsonResult;
            } catch (final Exception e) {
                return null;
            }

        }

        /**
         * if attribute is null, then the jsonResult must be an array
         * Else, this is a map AND the jsonResult.get(attribut) is an array
         * 
         * @param attribut
         * @return
         */
        public JSONArray getJsonArray(String attribut) {
            try {
                Object itemToCheck = jsonResult;
                if (attribut != null) {
                    itemToCheck = ((JSONObject) itemToCheck).get(attribut);
                }

                return (JSONArray) itemToCheck;
            } catch (final Exception e) {
                return null;
            }

        }

        public void checkResultFormat(String attribut, final boolean formatExpectedIsArray, final String message) {

            if (formatExpectedIsArray && getJsonArray(attribut) == null) {
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

    public String getUserName() {
        return mUserName;
    }

    /**
     * serialization
     * 
     * @return
     */
    public Map<String, Object> getMap() {
        Map<String, Object> result = new HashMap<>();

        result.put("username", mUserName);
        result.put("password", mPassword);
        result.put("urlrepo", mUrlRepository);

        return result;

    }

    /**
     * only for the getInstanceFromMap
     * Default Constructor.
     */
    private GithubAccessor() {
    };

    /**
     * serialization
     * 
     * @return
     */
    public static GithubAccessor getInstanceFromMap(Map<String, Object> source) {
        GithubAccessor githubAccessor = new GithubAccessor();
        githubAccessor.mUserName = (String) source.get("username");
        githubAccessor.mPassword = (String) source.get("password");
        githubAccessor.mUrlRepository = (String) source.get("urlrepo");
        return githubAccessor;
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
                mDefaultHeaderList,
                null,
                mUserName,
                mPassword);
        resultLastContrib.listEvents.addAll(restRequest.listEvents);
        if (BEventFactory.isError(restRequest.listEvents)) {
            return resultLastContrib;
        }
        try {
            final RESTResponse response = RESTCall.execute(restRequest, CONNECTION_TIMEOUT);
            if (logBox.isLog(LOGLEVEL.INFO)) {
                String bodyAnswer = response.getCollectOutput()!=null ? response.getCollectOutput().getBody() : null;
                // squid:S3358
                String logBody;
                if (bodyAnswer==null)
                    logBody="null";
                else 
                    logBody=bodyAnswer.length() > 50 ? bodyAnswer.substring(0, 50) + "..." : bodyAnswer;
                logBody = "Url["+orderGithub + "] Code:"+ response.getStatusCode() + " body:" + logBody;
                logBox.log(LOGLEVEL.INFO, "GithubAccess.getRestOrder: "+logBody);
            }

            if (response.getStatusCode() == 403) {
                String message = null;
                try {
                    resultLastContrib.jsonResult = response.getCollectOutput().getJson();
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

            resultLastContrib.jsonResult = response.getCollectOutput().getJson();
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
                mDefaultHeaderList,
                null,
                mUserName,
                mPassword);
        try {
            final RESTResponse response = RESTCall.execute(restRequest, CONNECTION_TIMEOUT);
            // System.out.println(response2.getBody());
            resultLastContrib.content = response.getCollectOutput().getBody();
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
                mDefaultHeaderList,
                null,
                mUserName,
                mPassword);
        restRequest.getCollectOutput().setPolicy(POLICYOUTPUT.BYTEARRAY);
        restRequest.setRedirect(true);
        try {
            final RESTResponse response = RESTCall.execute(restRequest, CONNECTION_TIMEOUT);
            // System.out.println(response.getBody());
            resultLastContrib.contentByte = response.getCollectOutput().getBaos().toByteArray();
            resultLastContrib.content = null;
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

    public static class ItemHeader {

        public String name;
        public String value;

        public static ItemHeader getItemHeader(String name, String value) {
            ItemHeader item = new ItemHeader();
            item.name = name;
            item.value = value;
            return item;
        }
    }

    /**
     * @param url
     * @param method
     * @param body
     * @param contentType
     * @param charset
     * @param headerList : each item contains 2 rows : headerName, HeaderValue
     * @param cookieList
     * @param username
     * @param password
     * @return
     */
    private RESTRequest buildRestRequest(final String url, final String method, final String body, final String contentType, final String charset,
            final ArrayList<ItemHeader> headerList,
            final ArrayList<ItemHeader> cookieList,
            final String username, final String password) {
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
        request.setContent(content);
        request.setBody(body);
        request.setRestMethod(RESTHTTPMethod.getRESTHTTPMethodFromValue(method));
        //request.setRedirect(true);
        //request.setIgnore(false);
        if (headerList != null) {
            for (final ItemHeader urlheader : headerList) {
                request.addHeader(urlheader.name, urlheader.value);
            }
        }
        if (cookieList != null) {
            for (final ItemHeader urlCookie : cookieList) {
                request.addCookie(urlCookie.name, urlCookie.value);
            }
        }
        if (username != null) {
            request.setAuthorization(buildBasicAuthorization(username, password));
        }
        request.setCollectOutput(CollectOutput.getInstanceString());
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
