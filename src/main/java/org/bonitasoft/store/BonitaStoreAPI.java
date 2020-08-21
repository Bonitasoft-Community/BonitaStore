package org.bonitasoft.store;

import java.io.File;

import org.bonitasoft.engine.session.APISession;

public class BonitaStoreAPI {

    public static BonitaStoreAPI getInstance() {
        return new BonitaStoreAPI();
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* getStore */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    /** access the Bonita community */
    /**
     * all information to access the Community store
     */
    public static final String CommunityGithubUserName = "bonitafoodtruck";
    public static final String CommunityGithubPassword = "!&Bonita2020!!";
    
    // https://developer.github.com/changes/2020-02-14-deprecating-password-auth/
    public static final String COMMUNITY_GITHUB_TOKEN = "38cf002243d47eade2912e5652ee739855bde97c";
    
    public static String CommunityGithubUrlRepository = "https://api.github.com/orgs/Bonitasoft-Community";

    public BonitaStore getBonitaCommunityStore(boolean registerTheStore) {
        return getBonitaStoreFactory().getBonitaCommunityStore(registerTheStore);
    }

    /**
     * Access a specific repository in the community
     * 
     * @param specificRepository
     * @return
     */
    public BonitaStore getBonitaCommunityStore(String specificRepository, boolean registerTheStore) {
        return getBonitaStoreFactory().getBonitaCommunityStore(specificRepository, registerTheStore);
    }

    public BonitaStoreGit getGitStore(String gituserName, String gitPassword, String gitUrlRepository,boolean registerTheStore) {
        return getBonitaStoreFactory().getGitStore(gituserName, gitPassword, gitUrlRepository, registerTheStore);
    }

    /*
     * get a store from a local disk
     */
    public BonitaStoreDirectory getDirectoryStore(File pathDirectory,boolean registerTheStore) {
        return getBonitaStoreFactory().getDirectoryStore(pathDirectory, registerTheStore);    }

    /*
     * get a store from a local disk
     */
    public BonitaStoreLocalServer getLocalStore(APISession apiSession) {
        BonitaStoreLocalServer bonitaDirectory = new BonitaStoreLocalServer(apiSession);
        return bonitaDirectory;
    }

    /**
     * return the BonitaStoreFactory. The factory is not unique.
     * 
     * @return
     */
    public BonitaStoreFactory getBonitaStoreFactory() {
        return BonitaStoreFactory.getInstance();
    }

}
