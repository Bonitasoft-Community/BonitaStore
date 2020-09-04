package org.bonitasoft.store;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.session.APISession;

public class BonitaStoreFactory {

    protected static BonitaStoreFactory getInstance() {
        return new BonitaStoreFactory();
    }

    
    /**
     * get a store from a serialization information
     * Each BonitaStore has a GetMap() function, to serialize the source. This method is the oposite, to recreate the source form the serialization
     * Each store setted a type
     * @param sourceOb
     * @return
     */
    public BonitaStore getBonitaStore(Map<String, Object> source) {
        // we have to parse all the different know store

        BonitaStore store;
        if ((store = BonitaStoreDirectory.getInstancefromMap(source))!=null)
            return store;
        if ((store = BonitaStoreGit.getInstancefromMap(source))!=null)
            return store;
        if ((store = BonitaStoreBCD.getInstancefromMap(source))!=null)
            return store;
     
        // Community
        
        // LocalServer
        
        // BonitaExternalServer
        if ((store = BonitaStoreBonitaExternalServer.getInstancefromMap(source))!=null)
            return store;
     
        
        return null;
    }
    
    
    
    
    private List<BonitaStore> listBonitaStore = new ArrayList<>();

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

    public BonitaStoreDirectory getDirectoryStore(File pathDirectory,boolean registerTheStore) {
        BonitaStoreDirectory bonitaStoreDirectory = new BonitaStoreDirectory(pathDirectory);
        if (registerTheStore)
            registerStore(bonitaStoreDirectory);
        return bonitaStoreDirectory;
        
    }

        
    /**
     * return the local server as a local store
     * 
     * @return
     */
    public BonitaStoreLocalServer getLocalServer(APISession apiSession, boolean registerTheStore) {
        return new BonitaStoreLocalServer(apiSession);
    }

    /**
     * return an external BonitaServer as a store
     * 
     * @return
     */
    public BonitaStoreBonitaExternalServer getBonitaExternalServer(String protocol, String server, int port, String applicationName, String userName, String password, boolean registerTheStore) {
        BonitaStoreBonitaExternalServer bonitaServer = new BonitaStoreBonitaExternalServer( protocol, server, port, applicationName, userName, password);
        if (registerTheStore)
            registerStore(bonitaServer);
        return bonitaServer;
    }

}
