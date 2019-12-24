package org.bonitasoft.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.session.APISession;

public class BonitaStoreFactory {

    protected static BonitaStoreFactory getInstance() {
        return new BonitaStoreFactory();
    }

    private List<BonitaStore> listBonitaStore = new ArrayList<BonitaStore>();

    public List<BonitaStore> getBonitaStores() {
        return listBonitaStore;
    }

    public void registerStores(List<BonitaStore> listBonitaStore) {
        this.listBonitaStore.addAll(listBonitaStore);
    }

    public void registerStore(BonitaStore bonitaStore) {
        // register only if not already present
        for (BonitaStore storeInTheList : this.listBonitaStore)
            if (storeInTheList.getId().equals(bonitaStore.getId()))
                return;

        this.listBonitaStore.add(bonitaStore);
    }

    /**
     * get a store from a serialisation information
     * 
     * @param sourceOb
     * @return
     */
    public BonitaStore getBonitaStore(Map<String, Object> sourceOb) {
        return null;
    }

    /**
     * getStoreByName
     * @param storeName
     * @return
     */
    public BonitaStore getStoreByName(String storeName) {
        if (storeName==null)
            return null;
        for (BonitaStore bonitaStore : listBonitaStore)
            if (storeName.equals( bonitaStore.getName()))
                return bonitaStore;
        return null;
    }

    /**
     * Static method, create the object but not add it in the list of store manipulate by the factory.
     * A registerStore is possible after if the registerTheStore is false
     * 
     * @return
     */
    public BonitaStoreCommunity getBonitaCommunityStore(boolean registerTheStore) {
        BonitaStoreCommunity bonitaStore = new BonitaStoreCommunity(BonitaStoreAPI.CommunityGithubUserName, BonitaStoreAPI.CommunityGithubPassword, BonitaStoreAPI.CommunityGithubUrlRepository);
        if (registerTheStore)
            registerStore(bonitaStore);
        return bonitaStore;
    }

    /**
     * get one specific repository in the Community repository
     * A registerStore is possible after if the registerTheStore is false
     * 
     * @param specificRepository
     * @return
     */
    public BonitaStoreCommunity getBonitaCommunityStore(String specificRepository, boolean registerTheStore) {
        BonitaStoreCommunity bonitaStoreCommunity = new BonitaStoreCommunity(BonitaStoreAPI.CommunityGithubUserName, BonitaStoreAPI.CommunityGithubPassword, specificRepository);
        bonitaStoreCommunity.setSpecificRepository(specificRepository);
        if (registerTheStore)
            registerStore(bonitaStoreCommunity);
        return bonitaStoreCommunity;
    }

    /**
     * get a store using Git
     * 
     * @param gituserName
     * @param gitPassword
     * @param gitUrlRepository
     * @return
     */
    public BonitaStoreGit getGitStore(String gituserName, String gitPassword, String gitUrlRepository, boolean registerTheStore) {
        BonitaStoreGit bonitaStoreGit = new BonitaStoreGit(gituserName, gitPassword, gitUrlRepository);
        if (registerTheStore)
            registerStore(bonitaStoreGit);
        return bonitaStoreGit;
    }

    /**
     * return the local server as a local store
     * 
     * @return
     */
    public BonitaStore getLocalServer(APISession apiSession, boolean registerTheStore) {
        return new BonitaStoreLocalServer(apiSession);
    }

    /**
     * return an external BonitaServer as a store
     * 
     * @return
     */
    public BonitaStore getBonitaServer(String server, int port, String applicationName, boolean registerTheStore) {
        return null;
    }

}
