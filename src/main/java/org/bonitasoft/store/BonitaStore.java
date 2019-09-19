package org.bonitasoft.store;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.ArrayStack;
import org.bonitasoft.store.BonitaStore.UrlToDownload;
import org.bonitasoft.store.artefact.Artefact;
import org.bonitasoft.store.artefact.Artefact.TypeArtefact;
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

        public List<TypeArtefact> listTypeArtefact = Arrays.asList(TypeArtefact.CUSTOMPAGE, TypeArtefact.CUSTOMWIDGET, TypeArtefact.GROOVY, TypeArtefact.PROCESS, TypeArtefact.BDM, TypeArtefact.LAYOUT, TypeArtefact.LIVINGAPP, TypeArtefact.THEME, TypeArtefact.RESTAPI, TypeArtefact.PROFILE,
                TypeArtefact.ORGANIZATION, TypeArtefact.LOOKANDFEEL);
        public boolean withNotAvailable = true;

        public boolean isByTopics = true;
    }

    public abstract StoreResult getListArtefacts(DetectionParameters detectionParameters, final LoggerStore logBox);

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
    public abstract StoreResult downloadArtefact(final Artefact artefactItem, UrlToDownload urlToDownload, /*
                                                                                                            * boolean
                                                                                                            * isBinaryContent,
                                                                                                            */ final LoggerStore logBox);

    /**
     * check if the sore it available, and can be reach
     * 
     * @param logBox
     * @return
     */
    public abstract StoreResult ping(LoggerStore logBox);

}
