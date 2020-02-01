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
    public static String CommunityGithubUserName = "bonitafoodtruck";
    public static String CommunityGithubPassword = "!&Bonita2020!!";
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

    public BonitaStoreGit getGitStore(String gituserName, String gitPassword, String gitUrlRepository) {
        BonitaStoreGit bonitaStoreGit = new BonitaStoreGit(gituserName, gitPassword, gitUrlRepository);
        return bonitaStoreGit;
    }

    /*
     * get a store from a local disk
     */
    public BonitaStoreDirectory getDirectoryStore(File pathDirectory) {
        BonitaStoreDirectory bonitaDirectory = new BonitaStoreDirectory(pathDirectory);
        return bonitaDirectory;
    }

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
