package org.bonitasoft.store;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
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
     * 
     * @param sourceOb
     * @return
     */
    public BonitaStore getBonitaStore(Map<String, Object> source) {
        // we have to parse all the different know store

        BonitaStore store;
        if ((store = BonitaStoreDirectory.getInstancefromMap(source)) != null)
            return store;
        if ((store = BonitaStoreGit.getInstancefromMap(source)) != null)
            return store;
        if ((store = BonitaStoreBCD.getInstancefromMap(source)) != null)
            return store;

        // Community

        // LocalServer

        // BonitaExternalServer
        if ((store = BonitaStoreBonitaExternalServer.getInstancefromMap(source)) != null)
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
     * 
     * @param storeName
     * @return
     */
    public BonitaStore getStoreByName(String storeName) {
        if (storeName == null)
            return null;
        for (BonitaStore bonitaStore : listBonitaStore)
            if (storeName.equals(bonitaStore.getName()))
                return bonitaStore;
        return null;
    }

    /**
     * Static method, create the object but not add it in the list of store manipulate by the factory.
     * A registerStore is possible after if the registerTheStore is false
     * 
     * @return
     */
    public BonitaStoreCommunity getInstanceBonitaCommunityStore(boolean registerTheStore) {
        BonitaStoreCommunity bonitaStore = new BonitaStoreCommunity( BonitaStoreCommunity.COMMUNITY_GITHUBURLREPOSITORY);
        if (registerTheStore)
            registerStore(bonitaStore);
        return bonitaStore;
    }

    /**
     * get one specific repository in the Community repository
     * A registerStore is possible after if the registerTheStore is false
     * 
     * @param specificRepository : for example  "https://api.github.com/repos/Bonitasoft-Community/page_towtruck"
     * @return
     */
    public BonitaStoreCommunity getInstanceBonitaCommunityStore(String specificRepository, boolean registerTheStore) {
        BonitaStoreCommunity bonitaStoreCommunity = new BonitaStoreCommunity( specificRepository);

        bonitaStoreCommunity.setSpecificFolder(specificRepository);
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
    public BonitaStoreGit getInstanceGitStore(String gituserName, String gitPassword, String gitUrlRepository, boolean registerTheStore) {
        BonitaStoreGit bonitaStoreGit = new BonitaStoreGit(gituserName, gitPassword, gitUrlRepository);
        
        if (registerTheStore)
            registerStore(bonitaStoreGit);
        return bonitaStoreGit;
    }

    public BonitaStoreDirectory getInstanceDirectoryStore(File pathDirectory, boolean registerTheStore) {
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
    public BonitaStoreLocalServer getInstanceLocalServer(APISession apiSession, boolean registerTheStore) {
        return new BonitaStoreLocalServer(apiSession);
    }

    /**
     * return an external BonitaServer as a store
     * The Bonita server keep one connection per client (it save in the tomcat session the connection).
     * The bonitaStoreBonitaExternalServer keeps the login information (cookie). So, if a second thread contact the same server, and reconnect, it will
     * disconnect the first connnection
     * That's why, when a BonitaServer is contacted, to use every time the same object to speak with.
     * Then, this object connect, saved cookies, and can be reused for all discussions.
     * Problem: if two thread want to connect with different userName, we must reconnect, then lost the first connection. So, connected to a BonitaServer with
     * two different userName is not possible
     * in a Multithread environement (and this is due to the Bonita Server, which saved on its part the connection information).
     * 
     * @return
     */
    Map<String, BonitaStoreBonitaExternalServer> mapBonitaExternalServer = new HashMap<>();

    public BonitaStoreBonitaExternalServer getInstanceBonitaExternalServer(String protocol, String server, int port, String applicationName, String userName, String password, boolean registerTheStore) {
        String keyStore = protocol + "#" + server + "#" + port + "#" + applicationName + "#" + userName;
        if (mapBonitaExternalServer.containsKey(keyStore))
            return mapBonitaExternalServer.get(keyStore);
        BonitaStoreBonitaExternalServer bonitaServer = new BonitaStoreBonitaExternalServer(protocol, server, port, applicationName, userName, password);
        if (registerTheStore)
            registerStore(bonitaServer);
        mapBonitaExternalServer.put(keyStore, bonitaServer);
        return bonitaServer;
    }

}
