package org.bonitasoft.store.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.client.CookieStore;

/**
 * This class reflects the information for a REST response.
 */
public class RESTResponse {

    /**
     * Reference the collectOutput given at the RestRequest
     */
    private CollectOutput collectOutput;

    /**
     * The execution time.
     */
    private long executionTime = 0L;

    /**
     * The HTTP status code.
     */
    private int statusCode = -1;

    /**
     * The information message.
     */
    private String message = "";

    /**
     * The headers.
     */
    // private List<RESTResultKeyValueMap> headers = new ArrayList<RESTResultKeyValueMap>();
    private List<Header> listHeaders = new ArrayList<>();

    private CookieStore cookieStore;

    /**
     * collectOutput value getter.
     * 
     * @return The colectOuput given at the RestRequest. When no CollectOutput was given, a default
     */
    public CollectOutput getCollectOutput() {
        return collectOutput;
    }

    /**
     * Execution time value getter.
     * 
     * @return The execution time value.
     */
    public long getExecutionTime() {
        return executionTime;
    }

    /**
     * Execution time value setter.
     * 
     * @param executionTime The new execution time value.
     */
    public void setExecutionTime(final long executionTime) {
        this.executionTime = executionTime;
    }

    /**
     * Status code value getter.
     * 
     * @return The status code value.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Status code value setter.
     * 
     * @param statusCode The new status code value.
     */
    public void setStatusCode(final int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Message value getter.
     * 
     * @return The message value.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Message value setter.
     * 
     * @param message The new message value.
     */
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
     * Headers value getter.
     * 
     * @return The headers value.
     */
    // public List<RESTResultKeyValueMap> getHeaders() {
    //     return headers;
    // }
    public Map<String, String> getHeadersMap() {
        Map<String, String> headersMap = new HashMap<>();
        for (Header header : listHeaders)
            headersMap.put(header.getName(), header.getValue());
        return headersMap;
    }

    public List<Header> getHeaders() {
        return listHeaders;
    }

    /**
     * Headers value setter.
     * 
     * @param headers The new headers value.
     */
    // public void setHeaders(final List<RESTResultKeyValueMap> headers) {
    //        this.headers = headers;
    // }
    public void setHeaders(List<Header> headers) {
        this.listHeaders = headers;
    }

    /**
     * Add a header couple in the headers.
     * 
     * @param key The key of the new header.
     * @param value The lonely value of the new header.
     * @return True if the header has been added or false otherwise.
     */
    public void addHeader(Header header) {
        listHeaders.add(header);

        /*
         * if (headers != null) {
         * final RESTResultKeyValueMap restResultKeyValueMap = new RESTResultKeyValueMap();
         * restResultKeyValueMap.setKey(key);
         * final List<String> values = new ArrayList<>();
         * values.add(value);
         * restResultKeyValueMap.setValue(values);
         * headers.add(restResultKeyValueMap);
         * return true;
         * }
         * return false;
         */
    }

    public void addHeaders(List<Header> header) {
        listHeaders.addAll(header);
    }

    public String getUrlRedirect() {
        String status = null;
        String location = null;
        for (final Header hv : listHeaders) {
            if (hv.getName().equals("Status")) {
                status = hv.getValue();
            }
            if (hv.getName().equals("Location")) {
                location = hv.getValue();
            }
        }

        if (statusCode == 301
                || statusCode == 302
                || (status != null && (status.startsWith("301") || status.startsWith("302")))) {
            return location;
        }

        return null;
    }

    /**
     * @param cookieStore
     */
    public void setCookieStore(CookieStore cookieStore) {
        this.cookieStore = cookieStore;
    }

    public CookieStore getCookieStore() {
        return cookieStore;
    }

    /**
     * A RestCall may have some redirection. Get the list of all URL called
     */
    private List<String> historyCall = new ArrayList<>();

    public void setHistoryCall(List<String> historyCall) {
        this.historyCall = historyCall;
    }

    public List<String> getHistoryCall() {
        return historyCall;
    }

    public void addHistoryCall(String call) {
        this.historyCall.add(call);
    }
}
