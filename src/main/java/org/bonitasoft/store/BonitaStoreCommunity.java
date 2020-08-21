package org.bonitasoft.store;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.store.BonitaStore.UrlToDownload;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.FactoryArtifact;
import org.bonitasoft.store.artifact.Artifact.TypeArtifact;
import org.bonitasoft.store.source.git.GithubAccessor.ResultGithub;
import org.bonitasoft.store.toolbox.LoggerStore;
import org.bonitasoft.store.toolbox.LoggerStore.LOGLEVEL;
import org.json.simple.JSONObject;

/* ******************************************************************************** */
/*                                                                                  */
/* BonitaStoreCommunity */
/*                                                                                  */
/* A Community Store contains multiple repository. Each repository contains multiple */
/* releases. One repository produce one artefact */
/*                                                                                  */
/* Example: foodtruck to get access on all the source */
/*                                                                                  */
/* ******************************************************************************** */
public class BonitaStoreCommunity extends BonitaStoreGit {

    private final static BEvent NoListingFound = new BEvent(BonitaStoreCommunity.class.getName(), 2, Level.APPLICATIONERROR, "No Listing Found", "The Githbub repository is supposed to have a file 'listing.xml' which describe all objects available. THis file is not found.",
            "result is not consistent",
            "Check the github repository, you may be not access the correct one ? ");

    private final static BEvent BadListingFormat = new BEvent(BonitaStoreCommunity.class.getName(), 3, Level.APPLICATIONERROR, "Bad Listing.xml format", "The format of the file Listing.xml is incorrect. This file describe all applications available in the repository",
            "The list of apps is not visible", "Check the exception, call the Github Administrator");
    private final static BEvent NoGithubInformation = new BEvent(BonitaStoreCommunity.class.getName(), 4, Level.APPLICATIONERROR, "NoGithub information", "To connect to Gitghub, a login/password is mandatory", "The repository can't be accessed, the list of apps present itself is not visible",
            "Give a login / password");

    private final static BEvent noContribFile = new BEvent(BonitaStoreCommunity.class.getName(), 5, Level.APPLICATIONERROR, "No contrib file", "The apps does not have any contrib file, and nothing can be download", "The apps can not be upload", "Contact the owner of the page to fix it");

    private final static BEvent noBinaryFile = new BEvent(BonitaStoreCommunity.class.getName(), 6, Level.APPLICATIONERROR, "No binary file", "The binary file can't be uploaded from the store", "Apps can not be upload", "Contact the owner of the page to fix it");

    private final static BEvent errorDecodeLogo = new BEvent(BonitaStoreCommunity.class.getName(), 7, Level.APPLICATIONERROR, "Error decode Logo", "The Logo can't be decode, or at not present", "No logo image", "Contact the owner of the page to fix it");

    private final static BEvent errorDecodePageProperties = new BEvent(BonitaStoreCommunity.class.getName(), 8, Level.APPLICATIONERROR, "Error decode Pageproperties", "The page.properties file can't be decode",
            "No description on the item in the repository, and then a local application and the repository one can be considered different when in fact this is the same", "Contact the owner of the page to fix it");

    private final static BEvent ERROR_READREPOSITORY = new BEvent(BonitaStoreCommunity.class.getName(), 9, Level.ERROR, "Error during reading repository", "Check errors",
            "Error detected during access to the repository", "Contact administrator");

    private final static BEvent EVENT_BAD_ARTEFACT = new BEvent(BonitaStoreCommunity.class.getName(), 10, Level.APPLICATIONERROR, "Bad Artefact", "Artefact is not the one expected according the topic",
            "A topic is given in the repository. The artefact is not the correct one according the topic", "Topic in the repository has to be fixed");

    public BonitaStoreCommunity(final String userName, final String password, final String urlRepository) {
        super(userName, password, urlRepository);
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

    public String specificRepository = null;

    public void setSpecificRepository(String specificRepository) {
        this.specificRepository = specificRepository;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(CST_BONITA_STORE_TYPE, "BonitaStoreCommunity");
        return map;
    }

    /**
     * Get all repository, wich must have a special structure
     */
    @Override
    public BonitaStoreResult getListArtifacts(DetectionParameters detectionParameters, LoggerStore logBox) {

        // public StoreResult getListArtefacts(final List<TypeArtefact> listTypeApps, boolean withNotAvailable, final LoggerStore logBox) {
        final SimpleDateFormat sdfParseRelease = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        FactoryArtifact factoryArtefact = FactoryArtifact.getInstance();

        final BonitaStoreResult storeResult = new BonitaStoreResult("getListAvailableItems");

        if (logBox.isLog(LOGLEVEL.MAIN)) {
            logBox.log(LOGLEVEL.MAIN, "Github  getListAvailableItems : " + mGithubAccessor.toLog());
        }
        String urlCall = null;
        String urlCompleteCall = null;
        String attributItem = null;
        boolean needMatchByName = true;
        try {
            if (!mGithubAccessor.isCorrect()) {
                storeResult.addEvent(NoGithubInformation);
                storeResult.endOperation();
                return storeResult;
            }
            // call the github to get the contents

            for (TypeArtifact typeArtefact : detectionParameters.listTypeArtifact) {
                TopicTypeArtefact topicTypeArtefact = new TopicTypeArtefact(typeArtefact);
                if (this.specificRepository == null) {
                    // community is organized by topics // if (detectionParameters.isByTopics)
                    //   https://api.github.com/search/repositories?page=1&per_page=100&q=org:Bonitasoft-Community+topic:page
                    //   https://api.github.com/orgs/Bonitasoft-Community/repos?page=1&per_page=10000

                    urlCompleteCall = "https://api.github.com/search/repositories?page=1&per_page=100&q=org:Bonitasoft-Community+topic:" + topicTypeArtefact.getTopicName();
                    attributItem = "items";
                    needMatchByName = false;
                    //  urlCall = "/repos?page=1&per_page=10000";
                } else
                    urlCall = this.specificRepository;

                final ResultGithub resultListRepository = mGithubAccessor.executeGetRestOrder(urlCall, urlCompleteCall, logBox);
                storeResult.addEvents(resultListRepository.listEvents);

                resultListRepository.checkResultFormat(attributItem, true, "The Github should return a list of repository");
                // Sarch all project started by the ItemTypetype
                if (resultListRepository.isError()) {
                    storeResult.endOperation();
                    return storeResult;
                }
                for (final Object oneRepositoryOb : resultListRepository.getJsonArray(attributItem)) {
                    if (!(oneRepositoryOb instanceof JSONObject)) {
                        continue;
                    }
                    final JSONObject oneRepository = (JSONObject) oneRepositoryOb;

                    final String repositoryName = (String) oneRepository.get("name");
                    TypeArtifact checkTypeArtefact = needMatchByName ? match(detectionParameters.listTypeArtifact, repositoryName) : typeArtefact;
                    if (checkTypeArtefact == null || checkTypeArtefact != typeArtefact) {
                        BEvent event = new BEvent(EVENT_BAD_ARTEFACT, "Repository [" + repositoryName + "] expect type[" + typeArtefact + "] detect [" + checkTypeArtefact + "]");
                        logBox.log(event);
                        storeResult.addEvent(event);
                        continue;
                    }
                    // this is what we search

                    final Artifact artefactItem = factoryArtefact.getFromType(typeArtefact, (String) oneRepository.get("name"), null,
                            (String) oneRepository.get("description"),
                            null,
                            this);

                    if (artefactItem == null) {
                        logBox.log(LoggerStore.LOGLEVEL.ERROR, "Artefact is null from type[" + typeArtefact + "]");
                        continue;

                    }
                    // set the name. If a page.properties exist, then we will get the
                    // information inside
                    String traceOneApps = "name[" + artefactItem.getName() + "]";
                    String shortName = artefactItem.getName();
                    if (shortName.toUpperCase().startsWith(typeArtefact.toString().toUpperCase() + "_")) {
                        // remove the typeApps
                        shortName = shortName.substring(typeArtefact.toString().length() + 1);
                    }
                    if (shortName.toUpperCase().startsWith(topicTypeArtefact.getTopicName().toUpperCase() + "_")) {
                        // remove the typeApps
                        shortName = shortName.substring(topicTypeArtefact.getTopicName().length() + 1);
                    }

                    // --------------------- Content
                    // Logo and documentation
                    artefactItem.urlContent = (String) oneRepository.get("url");

                    final ResultGithub resultContent = mGithubAccessor.executeGetRestOrder(null, artefactItem.urlContent + "/contents", logBox);
                    resultContent.checkResultFormat(null, true, "Contents of a repository must be a list");
                    storeResult.addEvents(resultContent.listEvents);

                    if (!resultContent.isError()) {
                        for (final Map<String, Object> oneContent : (List<Map<String, Object>>) resultContent.getJsonArray(null)) {
                            final String assetName = (String) oneContent.get("name");
                            if (assetName == null) {
                                continue;
                            }
                            if (assetName.equalsIgnoreCase("page.properties")) {
                                final String urlPageProperties = (String) oneContent.get("download_url");
                                final ResultGithub resultContentpage = mGithubAccessor.getContent(urlPageProperties, "GET", "", "UTF-8");
                                storeResult.addEvents(resultContentpage.listEvents);
                                if (!BEventFactory.isError(resultContentpage.listEvents)) {
                                    final String pageSt = resultContentpage.content;
                                    final StringReader stringPage = new StringReader(pageSt);
                                    try {
                                        final Properties properties = new Properties();

                                        properties.load(stringPage);
                                        final String pageName = properties.getProperty("name");
                                        if (pageName != null && !pageName.isEmpty()) {
                                            artefactItem.setName(pageName);
                                        }

                                        final String pageDescription = properties.getProperty("description");
                                        if (pageDescription != null && !pageDescription.isEmpty()) {
                                            artefactItem.setDescription(pageDescription);
                                        }

                                        final String pageDisplayName = properties.getProperty("displayName");
                                        if (pageDisplayName != null && !pageDisplayName.isEmpty()) {
                                            artefactItem.setDisplayName(pageDisplayName);
                                        }
                                    } catch (final Exception e) {
                                        storeResult.addEvent(new BEvent(errorDecodePageProperties, "Error " + e.toString()));
                                    }
                                }

                            }

                            if (assetName.equalsIgnoreCase("logo.jpg") || assetName.equalsIgnoreCase(shortName + ".jpg")) {

                                // we get it !
                                try {
                                    final ResultGithub resultContentLogo = mGithubAccessor.executeGetRestOrder(null, (String) oneContent.get("url"), logBox);
                                    final String logoSt = (String) resultContentLogo.getJsonObject().get("content");
                                    final Base64 base64 = new Base64();
                                    artefactItem.logo = base64.decode(logoSt);
                                    traceOneApps += "logo detected;";
                                } catch (final Exception e) {
                                    artefactItem.addEvent(new BEvent(errorDecodeLogo, "Get logo from  [" + oneContent.get("url") + "] : " + e.toString()));
                                }
                            }
                            if (assetName.endsWith(".pdf")) {
                                // we get the documentation
                                artefactItem.documentationFile = (String) oneContent.get("url");
                                traceOneApps += "doc detected;";
                            }
                        } // end loop on content
                    } // end get Contents

                    // --------------------- release
                    String releaseUrl = (String) oneRepository.get("releases_url");
                    // release is : :
                    // "https://api.github.com/repos/Bonitasoft-Community/page_awacs/releases{/id}",
                    if (releaseUrl != null && releaseUrl.endsWith("{/id}")) {
                        releaseUrl = releaseUrl.substring(0, releaseUrl.length() - "{/id}".length());
                    }

                    // get the releases now
                    final ResultGithub resultRelease = mGithubAccessor.executeGetRestOrder(null, releaseUrl, logBox);
                    resultRelease.checkResultFormat(null, true, "Contents of a release must be a list");
                    storeResult.addEvents(resultRelease.listEvents);
                    if (!resultRelease.isError()) {
                        for (final Map<String, Object> oneRelease : (List<Map<String, Object>>) resultRelease.getJsonArray(null)) {

                            final Artifact.ArtefactRelease appsRelease = artefactItem.newInstanceRelease();
                            appsRelease.id = (Long) oneRelease.get("id");
                            appsRelease.version = oneRelease.get("name").toString();
                            traceOneApps += "release[" + appsRelease.version + "] detected;";

                            try {
                                appsRelease.dateRelease = sdfParseRelease.parse(oneRelease.get("published_at").toString());
                            } catch (final Exception e) {
                                logBox.log(LOGLEVEL.ERROR, "FoodTruckStoreGithub : date [" + oneRelease.get("published_at") + "] can't be parse.");
                            }
                            appsRelease.releaseNote = oneRelease.get("body").toString();
                            // search a ZIP access
                            if (oneRelease.get("assets") != null) {
                                for (final Map<String, Object> oneAsset : (List<Map<String, Object>>) oneRelease.get("assets")) {
                                    final String assetName = (String) oneAsset.get("name");
                                    if (assetName != null && assetName.endsWith(".zip")) {
                                        appsRelease.urlDownload = (String) oneAsset.get("browser_download_url");
                                        if (appsRelease.urlDownload != null && appsRelease.urlDownload.length() == 0) {
                                            appsRelease.urlDownload = null;
                                        }
                                        appsRelease.numberOfDownload = (Long) oneAsset.get("download_count");
                                        traceOneApps += "release with content;";
                                    }
                                }
                            }
                            artefactItem.listReleases.add(appsRelease);
                        } // end loop on release

                        if (artefactItem.listReleases.size() == 0 || artefactItem.getLastUrlDownload() == null) {
                            artefactItem.setAvailable(false);
                        }
                    }
                    logBox.log(LoggerStore.LOGLEVEL.INFO, traceOneApps);
                    if (!artefactItem.isAvailable()) {
                        if (detectionParameters.withNotAvailable)
                            storeResult.listArtifacts.add(artefactItem);
                    } else
                        storeResult.listArtifacts.add(artefactItem);
                } // end loop repo

                /*
                 * appsItem.contribFile = eElement.getAttribute("bonitacontributfile");
                 * appsItem.urlDownload = baseUrlDownload +
                 * eElement.getAttribute("bonitacontributfile");
                 * appsItem.documentationFile =
                 * eElement.getAttribute("documentationfile"); try {
                 * appsItem.releaseDate =
                 * sdfParseRelease.parse(eElement.getAttribute("releasedate").toString()
                 * ); } catch (final Exception e) {
                 * logger.severe("FoodTruckStoreGithub.getListAvailableApps Parse Date["
                 * + eElement.getAttribute("releasedate") + " error " + e); }
                 * appsItem.description =
                 * eElement.getElementsByTagName("description").item(0).getTextContent()
                 * ; // logo -------------- generated by GenerateListingItem.java final
                 * String logoSt =
                 * eElement.getElementsByTagName("logo").item(0).getTextContent(); if
                 * (logoSt != null && logoSt.length() > 0) { final Base64 base64 = new
                 * Base64(); appsItem.logo = base64.decode(logoSt); }
                 * storeResult.listCustomPage.add(appsItem); } } catch(final Exception
                 * e) { storeResult.addEvent(new BEvent(BadListingFormat, e, "")); }
                 */
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            storeResult.addEvent(new BEvent(ERROR_READREPOSITORY, e, "During Git[" + urlCall + "]"));
            logBox.log(LoggerStore.LOGLEVEL.ERROR, "Exception " + e.getMessage() + " During Git[" + urlCall + "] details " + exceptionDetails);
        }

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
            storeResult.addEvent(new BEvent(noContribFile, "Apps[" + artifact.getName() + "]"));
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

    // decorator
    public class TopicTypeArtefact {

        TypeArtifact typeArtefact;

        TopicTypeArtefact(TypeArtifact typeArtefact) {
            this.typeArtefact = typeArtefact;
        }

        public String getTopicName() {
            if (typeArtefact == TypeArtifact.CUSTOMPAGE)
                return "page";
            if (typeArtefact == TypeArtifact.RESTAPI)
                return "restapiextension";
            if (typeArtefact == TypeArtifact.CUSTOMWIDGET)
                return "widget";
            if (typeArtefact == TypeArtifact.LAYOUT)
                return "layout";
            if (typeArtefact == TypeArtifact.LOOKANDFEEL)
                return "lookandfeel";
            return "unknown";

        }
    }

}
