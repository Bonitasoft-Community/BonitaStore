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
    public static String CommunityGithubPassword = "bonita2016";
    public static String CommunityGithubUrlRepository = "https://api.github.com/orgs/Bonitasoft-Community";

    public BonitaStore getBonitaCommunityStore() {
        return BonitaStoreFactory.getBonitaCommunityStore();
    }

    /**
     * Access a specific repository in the community
     * 
     * @param specificRepository
     * @return
     */
    public BonitaStore getBonitaCommunityStore(String specificRepository) {
        return BonitaStoreFactory.getBonitaCommunityStore(specificRepository);
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
    public BonitaStoreLocal getLocalStore(APISession apiSession) {
        BonitaStoreLocal bonitaDirectory = new BonitaStoreLocal(apiSession);
        return bonitaDirectory;
    }
    /**
     * return the BonitaStoreFactory. The factory is not unique.
     * 
     * @return
     */
    public BonitaStoreFactory getNewBonitaStoreFactory() {
        return BonitaStoreFactory.getNewInstance();
    }

}
