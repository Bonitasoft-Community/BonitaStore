package org.bonitasoft.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.toolbox.LoggerStore;

public abstract class BonitaStore {

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Describe the store */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    public abstract String getName();

    public abstract String getExplanation();
    
    private String displayName;
    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName( String displayName ) {
        this.displayName = displayName;
    }
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
    public final static String CST_BONITA_STORE_TYPE = "type";
    public final static String CST_BONITA_STORE_DISPLAYNAME="displayname";
    public final static String CST_BONITA_STORE_NAME = "name";
    public final static String CST_BONITA_STORE_EXPLANATION="explanation";

    /**
     * Serialization. Note, the unserailisation is part of each Store
     * @return
     */
    public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put(CST_BONITA_STORE_TYPE, getType() );
            map.put(CST_BONITA_STORE_DISPLAYNAME, getDisplayName());
            map.put(CST_BONITA_STORE_NAME, getName());
            map.put(CST_BONITA_STORE_EXPLANATION, getExplanation());
            fullfillMap(map);
            return map;
        
    }
    protected abstract String getType();
    protected abstract void fullfillMap(Map<String, Object> map );

  
    /* ******************************************************************************** */
    /*                                                                                  */
    /* begin and end. Some store need to do some operation before using it              */
    /* (open a connection...)                                                           */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    public List<BEvent> begin(BonitaStoreParameters detectionParameters, LoggerStore logBox) {
        return new ArrayList<>();
    }
    
    public List<BEvent> end(BonitaStoreParameters detectionParameters, LoggerStore logBox) {
        return new ArrayList<>();
    }
    
    /* ******************************************************************************** */
    /*                                                                                  */
    /* Operation expected from a store */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    public abstract BonitaStoreResult getListArtifacts(BonitaStoreParameters detectionParameters, final LoggerStore loggerStore);

    public enum UrlToDownload {
        LASTRELEASE, URLCONTENT, URLDOWNLOAD
    };

    /**
     * Load one artifact
     * 
     * @param artefactItem
     * @param urlToDownload : which type of artifact to download? LastRealease? Specific one ?
     * @param logBox
     * @return
     */
    public abstract BonitaStoreResult loadArtifact(final Artifact artifact, UrlToDownload urlToDownload, final LoggerStore logBox);

    
    
    /**
     * check if the sore it available, and can be reach
     * 
     * @param logBox
     * @return
     */
    public abstract BonitaStoreResult ping(LoggerStore logBox);

}
