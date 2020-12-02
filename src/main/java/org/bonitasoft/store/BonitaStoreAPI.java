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
  
    public BonitaStore getInstanceBonitaCommunityStore(boolean registerTheStore) {
        return getBonitaStoreFactory().getInstanceBonitaCommunityStore(registerTheStore);
    }

    /**
     * Access a specific repository in the community
     * 
     * @param specificRepository
     * @return
     */
    public BonitaStoreGit getInstanceBonitaCommunityStore(String specificRepository, boolean registerTheStore) {
        return getBonitaStoreFactory().getInstanceBonitaCommunityStore(specificRepository, registerTheStore);
    }

    public BonitaStoreGit getInstanceGitStore(String gituserName, String gitPassword, String gitUrlRepository, boolean registerTheStore) {
        return getBonitaStoreFactory().getInstanceGitStore(gituserName, gitPassword, gitUrlRepository, registerTheStore);
    }

    /*
     * get a store from a local disk
     */
    public BonitaStoreDirectory getInstanceDirectoryStore(File pathDirectory, boolean registerTheStore) {
        return getBonitaStoreFactory().getInstanceDirectoryStore(pathDirectory, registerTheStore);
    }

    /*
     * get a store from a local disk
     */
    public BonitaStoreLocalServer getInstanceLocalStore(APISession apiSession) {
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
