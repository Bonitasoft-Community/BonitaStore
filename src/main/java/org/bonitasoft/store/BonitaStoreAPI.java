package org.bonitasoft.store;


public class BonitaStoreAPI {
  
  /* -------------------------------------------------------------------- */
  /*                                                                      */
  /* getStore */
  /*                                                                      */
  /* -------------------------------------------------------------------- */
  /** access the Bonita community */
  public static String CommunityGithubUserName = "bonitafoodtruck";
  public static String CommunityGithubPassword = "bonita2016";
  public static String CommunityGithubUrlRepository = "https://api.github.com/orgs/Bonitasoft-Community";


  public static BonitaStoreGit getBonitaCommunityStore()
  {
    return new BonitaStoreGit( CommunityGithubUserName, CommunityGithubPassword, CommunityGithubUrlRepository );
  }
  public static BonitaStoreGit getBonitaCommunityStore( String specificRepository)
  {
    BonitaStoreGit bonitaStoreGit = new BonitaStoreGit( CommunityGithubUserName, CommunityGithubPassword, CommunityGithubUrlRepository );
    bonitaStoreGit.setSpecificRepository( specificRepository );
    return bonitaStoreGit;
  }
  public static BonitaStoreGit getGitStore( String gituserName, String gitPassword, String gitUrlRepository)
  {
    BonitaStoreGit bonitaStoreGit = new BonitaStoreGit( gituserName, gitPassword, gitUrlRepository );
    return bonitaStoreGit;
  }

}
