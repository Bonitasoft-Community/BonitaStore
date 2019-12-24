package org.bonitasoft.store;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.Artifact.TypeArtifact;
import org.bonitasoft.store.toolbox.LoggerStore;

public abstract class BonitaStore {

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Describe the store */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    public abstract String getName();

    /**
     * return a unique ID to indentify the store. In a respository, it maybe the String + User Name to connect for example.
     * 
     * @return
     */
    public abstract String getId();

    /**
     * the store count the number of download
     * 
     * @return
     */
    public abstract boolean isManageDownload();

    /**
     * the store must be saved or instanciate, so each store must provide a Map with all the needed
     * information
     * 
     * @return
     */
    public static String BonitaStoreType = "Type";
    public static String BonitaStoreName = "name";

    public abstract Map<String, Object> toMap();

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Operation expected from a store */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    /**
     * source is a list of repository (a github repository for example)
     * 
     * @param listTypeApps
     * @param withNotAvailable
     * @param logBox
     * @return
     */
    public static class DetectionParameters {

        public List<TypeArtifact> listTypeArtifact = Arrays.asList(TypeArtifact.CUSTOMPAGE, TypeArtifact.CUSTOMWIDGET, TypeArtifact.GROOVY, TypeArtifact.PROCESS, TypeArtifact.BDM, TypeArtifact.LAYOUT, TypeArtifact.LIVINGAPP, TypeArtifact.THEME, TypeArtifact.RESTAPI, TypeArtifact.PROFILE,
                TypeArtifact.ORGANIZATION, TypeArtifact.LOOKANDFEEL);
        public boolean withNotAvailable = true;

        public boolean isByTopics = true;
    }

    public abstract BonitaStoreResult getListArtefacts(DetectionParameters detectionParameters, final LoggerStore loggerStore);

    public enum UrlToDownload {
        LASTRELEASE, URLCONTENT, URLDOWNLOAD
    };

    /**
     * download one artefact
     * 
     * @param artefactItem
     * @param urlToDownload : which type of artefact to download? LastRealease? Specific URL?
     * @param logBox
     * @return
     */
    public abstract BonitaStoreResult downloadArtefact(final Artifact artefactItem, UrlToDownload urlToDownload, /*
                                                                                                            * boolean
                                                                                                            * isBinaryContent,
                                                                                                            */ final LoggerStore logBox);

    /**
     * check if the sore it available, and can be reach
     * 
     * @param logBox
     * @return
     */
    public abstract BonitaStoreResult ping(LoggerStore logBox);

}
