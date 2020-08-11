package org.bonitasoft.store.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

/**
 * A basic cookie store to be used in a HTTP request.
 */
public class RESTCookieStore implements CookieStore {

    /**
     * The list of cookies of the store.
     */
    private final List<Cookie> cookies = new ArrayList<Cookie>();

    public void addCookie(final Cookie cookie) {
        cookies.add(cookie);
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public boolean clearExpired(final Date date) {
        return true;
    }

    public void clear() {
        cookies.clear();
    }

}
