package org.bonitasoft.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BonitaStoreFactory {

  protected static BonitaStoreFactory getNewInstance() {
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
    this.listBonitaStore.add(bonitaStore);
  }

  /**
   * Static method, create the object but not add it in the list of store manipulate by the factory.
   * A registerStore is mandatory
   * 
   * @return
   */
  public static BonitaStoreCommunity getBonitaCommunityStore() {
    return new BonitaStoreCommunity(BonitaStoreAPI.CommunityGithubUserName, BonitaStoreAPI.CommunityGithubPassword, BonitaStoreAPI.CommunityGithubUrlRepository);
  }

  /**
   * get one specific repository in the Community repository
   * 
   * @param specificRepository
   * @return
   */
  public static BonitaStoreCommunity getBonitaCommunityStore(String specificRepository) {
    BonitaStoreCommunity bonitaStoreCommunity = new BonitaStoreCommunity(BonitaStoreAPI.CommunityGithubUserName, BonitaStoreAPI.CommunityGithubPassword, specificRepository);
    bonitaStoreCommunity.setSpecificRepository(specificRepository);
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
  public static BonitaStoreGit getGitStore(String gituserName, String gitPassword, String gitUrlRepository) {
    BonitaStoreGit bonitaStoreGit = new BonitaStoreGit(gituserName, gitPassword, gitUrlRepository);
    return bonitaStoreGit;
  }

  /**
   * return the local server as a local store
   * 
   * @return
   */
  public static BonitaStore getLocalBonitaServer() {
    return null;
  }

  /**
   * return an external BonitaServer as a store
   * 
   * @return
   */
  public static BonitaStore getBonitaServer(String server, int port, String applicationName) {
    return null;
  }

}
