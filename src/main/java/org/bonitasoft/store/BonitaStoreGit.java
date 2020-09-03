package org.bonitasoft.store;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.Artifact.TypeArtifact;
import org.bonitasoft.store.artifact.FactoryArtifact;
import org.bonitasoft.store.source.git.GithubAccessor;
import org.bonitasoft.store.source.git.GithubAccessor.ResultGithub;
import org.bonitasoft.store.toolbox.LoggerStore;
import org.bonitasoft.store.toolbox.LoggerStore.LOGLEVEL;
import org.json.simple.JSONObject;

/* ******************************************************************************** */
/*                                                                                  */
/* BonitaStoreGit */
/*                                                                                  */
/* One repository, which contains multiple artefact. */
/*                                                                                  */
/* ******************************************************************************** */

public class BonitaStoreGit extends BonitaStore {

    protected GithubAccessor mGithubAccessor;

    private final static BEvent NoListingFound = new BEvent(BonitaStoreGit.class.getName(), 2, Level.APPLICATIONERROR, "No Listing Found", "The Githbub repository is supposed to have a file 'listing.xml' which describe all objects available. THis file is not found.",
            "result is not consistent",
            "Check the github repository, you may be not access the correct one ? ");

    private final static BEvent BadListingFormat = new BEvent(BonitaStoreGit.class.getName(), 3, Level.APPLICATIONERROR, "Bad Listing.xml format", "The format of the file Listing.xml is incorrect. This file describe all applications available in the repository",
            "The list of apps is not visible", "Check the exception, call the Github Administrator");
    private final static BEvent NoGithubInformation = new BEvent(BonitaStoreGit.class.getName(), 4, Level.APPLICATIONERROR, "NoGithub information", "To connect to Gitghub, a login/password is mandatory", "The repository can't be accessed, the list of apps present itself is not visible",
            "Give a login / password");

    private final static BEvent noContribFile = new BEvent(BonitaStoreGit.class.getName(), 5, Level.APPLICATIONERROR, "No contrib file", "The apps does not have any contrib file, and nothing can be download", "The apps can not be upload", "Contact the owner of the page to fix it");

    private final static BEvent noBinaryFile = new BEvent(BonitaStoreGit.class.getName(), 6, Level.APPLICATIONERROR, "No binary file", "The binary file can't be uploaded from the store", "Apps can not be upload", "Contact the owner of the page to fix it");

    private final static BEvent errorDecodeLogo = new BEvent(BonitaStoreGit.class.getName(), 7, Level.APPLICATIONERROR, "Error decode Logo", "The Logo can't be decode, or at not present", "No logo image", "Contact the owner of the page to fix it");

    private final static BEvent errorDecodePageProperties = new BEvent(BonitaStoreGit.class.getName(), 8, Level.APPLICATIONERROR, "Error decode Pageproperties", "The page.properties file can't be decode",
            "No description on the item in the repository, and then a local application and the repository one can be considered different when in fact this is the same", "Contact the owner of the page to fix it");

    public BonitaStoreGit(final String userName, final String password, final String urlRepository) {
        mGithubAccessor = new GithubAccessor(userName, password, urlRepository);
    }
   
    /**
     * return the name
     *
     * @return
     */
    @Override
    public String getName() {
        return mGithubAccessor.getUrlRepository();
    }
    @Override
    public String getExplanation() {
        return "Give information to connect Git Repository. All artifacts saved in this directory, as RELEASE, will be study to be deployed on this server.";
    }
    public String getId() {
        return "Git-" + mGithubAccessor.getUrlRepository() + "-" + mGithubAccessor.getUserName();
    }

    @Override
    public boolean isManageDownload() {
        return true;
    }

    public final static String CST_TYPE_GIT = "git";
    
    @Override 
    public String getType() {
        return CST_TYPE_GIT;
    }

    @Override
    public void fullfillMap( Map<String,Object> map) {
        map.put("gitaccessor", mGithubAccessor.getMap());
    }

    private BonitaStoreGit() {
        mGithubAccessor = null;
    }
    /**
     * 
     * @param source
     * @return
     */
    @SuppressWarnings("unchecked")
    public static BonitaStore getInstancefromMap(Map<String, Object> source) {
        try {
            String type = (String) source.get(CST_BONITA_STORE_TYPE);
            if (!CST_TYPE_GIT.equals(type))
                return null;
            BonitaStoreGit store = new BonitaStoreGit();
            store.mGithubAccessor = GithubAccessor.getInstanceFromMap((Map<String,Object>)source.get("gitaccessor")); 
            return store;
        } catch (Exception e) {
            return null;
        }

    }
    
    
    public String specificRepository = null;

    public void setSpecificRepository(String specificRepository) {
        this.specificRepository = specificRepository;
    }

    /**
     * Get all repository, wich must have a special structure
     */
    @Override
    public BonitaStoreResult getListArtifacts(BonitaStoreParameters detectionParameters, LoggerStore logBox) {
        final SimpleDateFormat sdfParseRelease = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        final BonitaStoreResult storeResult = new BonitaStoreResult("getListAvailableItems");

        if (logBox.isLog(LOGLEVEL.MAIN)) {
            logBox.log(LOGLEVEL.MAIN, "Github  getListAvailableItems : " + mGithubAccessor.toLog());
        }

        if (!mGithubAccessor.isCorrect()) {
            storeResult.addEvent(NoGithubInformation);
            storeResult.endOperation();
            return storeResult;
        }
        // call the github to get the contents
        String urlCall = this.specificRepository == null ? "/repos?page=1&per_page=10000" : this.specificRepository;

        final ResultGithub resultListRepository = mGithubAccessor.executeGetRestOrder(urlCall, null, logBox);
        storeResult.addEvents(resultListRepository.listEvents);

        resultListRepository.checkResultFormat(null, true, "The Github should return a list or repository");
        // Sarch all project started by the ItemTypetype
        if (resultListRepository.isError()) {
            storeResult.endOperation();
            return storeResult;
        }

        // check the list of all item returned
        for (final Object oneRepositoryOb : resultListRepository.getJsonArray(null)) {
            if (!(oneRepositoryOb instanceof JSONObject)) {
                continue;
            }
            final JSONObject oneRepository = (JSONObject) oneRepositoryOb;

            final String repositoryName = (String) oneRepository.get("name");
            TypeArtifact typeArtefact = match(detectionParameters.listTypeArtifacts, repositoryName);
            if (typeArtefact == null)
                continue;

            // this is what we search

            FactoryArtifact factoryArtefact = FactoryArtifact.getInstance();

            final Artifact artefactItem = factoryArtefact.getFromType(typeArtefact, (String) oneRepository.get("name"), null,
                    (String) oneRepository.get("description"),
                    null,
                    this);

            if (artefactItem == null) {
                continue;
            }

            // set the name. If a page.properties exist, then we will get the
            // information inside
            String traceOneApps = "name[" + artefactItem.getBonitaName() + "]";
            String shortName = artefactItem.getName();
            if (shortName.toUpperCase().startsWith(typeArtefact.toString().toUpperCase() + "_")) {
                // remove the typeApps
                shortName = shortName.substring(typeArtefact.toString().length() + 1);
            }

            // --------------------- Content
            // Logo and documentation
            artefactItem.urlContent = (String) oneRepository.get("url");
            artefactItem.urlDownload = (String) oneRepository.get("download_url");

            logBox.log(LoggerStore.LOGLEVEL.INFO, traceOneApps);
            if (!artefactItem.isAvailable()) {
                if (detectionParameters.withNotAvailable)
                    storeResult.addDetectedArtifact(detectionParameters, artefactItem);
            } else
                storeResult.addDetectedArtifact(detectionParameters, artefactItem);
        } // end loop repo

        storeResult.endOperation();
        return storeResult;
    }

    /**
     * check if the github repository match the expected type
     *
     * @param typeArtefact
     * @param repositoryName
     * @return the typeApp or null if nothing match
     */
    private TypeArtifact match(final List<TypeArtifact> listTypeApps, String repositoryName) {
        // TypeAppsName is CUSTOMPAGE or CUSTOMWIDGET

        // we accept CUSTOMPAGE or PAGE
        if (repositoryName == null) {
            return null;
        }
        repositoryName = repositoryName.toUpperCase();

        for (TypeArtifact typeApps : listTypeApps) {
            final String typeAppsName = typeApps.toString().toUpperCase();
            if (repositoryName.startsWith(typeAppsName + "_") || ("CUSTOM" + repositoryName).startsWith(typeAppsName + "_") || repositoryName.toUpperCase().endsWith(typeAppsName)) {
                return typeApps;
            }
        }
        return null;
    }

    @Override
    public BonitaStoreResult loadArtifact(final Artifact artifact, UrlToDownload urlToDownload, final LoggerStore logBox) {

        final BonitaStoreResult storeResult = new BonitaStoreResult("DownloadOneCustomPage");
        String url = null;
        switch (urlToDownload) {
            case URLCONTENT:
                url = artifact.urlContent;
                break;
            case LASTRELEASE:
                url = artifact.getLastUrlDownload();
                break;
            case URLDOWNLOAD:
                url = artifact.urlDownload;
                break;

        }
        if (url == null) {
            storeResult.addEvent(new BEvent(noContribFile, "Apps[" + artifact.getBonitaName() + "]"));
            return storeResult;
        }
        if (logBox.isLog(LOGLEVEL.MAIN)) {
            logBox.log(LOGLEVEL.MAIN, "Bonitastore:downloadOneCustomPage : download... [" + url + "]");
        }

        ResultGithub resultListing = null;
        if (artifact.isBinaryContent())
            resultListing = mGithubAccessor.getBinaryContent(url, "GET", null, null);
        else
            resultListing = mGithubAccessor.getContent(url, "GET", null, null);

        if (logBox.isLog(LOGLEVEL.MAIN)) {
            logBox.log(LOGLEVEL.MAIN, "Bonitastore:downloadOneCustomPage : end download... [" + url + "] status=" + resultListing.listEvents);
        }

        storeResult.addEvents(resultListing.listEvents);
        if (BEventFactory.isError(resultListing.listEvents)) {
            return storeResult;
        }
        // result is a String, save it in the byteArray
        storeResult.contentByte = resultListing.contentByte;
        storeResult.content = resultListing.content;

        return storeResult;
    }

    /**
     * /**
     * Ping the repository, to see if it is available
     * 
     * @return
     */
    public BonitaStoreResult ping(LoggerStore logBox) {
        final BonitaStoreResult storeResult = new BonitaStoreResult("ping");

        // public StoreResult getListArtefacts(final List<TypeArtefact> listTypeApps, boolean withNotAvailable, final LoggerStore logBox) {

        if (logBox.isLog(LOGLEVEL.MAIN)) {
            logBox.log(LOGLEVEL.MAIN, "Github  getListAvailableItems : " + mGithubAccessor.toLog());
        }

        if (!mGithubAccessor.isCorrect()) {
            storeResult.addEvent(NoGithubInformation);
            storeResult.endOperation();
            return storeResult;
        }
        // call the github to get the contents
        final ResultGithub resultRepos = mGithubAccessor.executeGetRestOrder("/repos", null, logBox);
        storeResult.addEvents(resultRepos.listEvents);
        return storeResult;
    }

}
