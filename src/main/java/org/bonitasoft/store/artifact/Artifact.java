package org.bonitasoft.store.artifact;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.bonitasoft.engine.bpm.BaseElement;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStore;
import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.artifactdeploy.DeployStrategy;
import org.bonitasoft.store.artifactdeploy.DeployStrategy.DeployOperation;
import org.bonitasoft.store.toolbox.LoggerStore;

/**
 * an AppsItem maybe a Custom Page, a CustomWidget, ...
 */
public abstract class Artifact {

    protected final static BEvent EventReadFile = new BEvent(Artifact.class.getName(), 1, Level.APPLICATIONERROR, "File error", "The file can't be read", "The artefact is ignored", "Check the exception");

    public enum TypeArtifact {
        CUSTOMPAGE, CUSTOMWIDGET, GROOVY, PROCESS, BDM, LAYOUT, LIVINGAPP, THEME, RESTAPI, PROFILE, ORGANIZATION, LOOKANDFEEL, ELSE
    };

    /**
     * name of the application. THis name is unique on the Store and locally
     */
    private String name;
    private String version;
    private Date dateVersion;
    protected String description;

    private TypeArtifact typeArtifact;

    /**
     * additionnal information
     */
    protected String displayName;
    protected String contribFile;

    /*
     * according the source, the artefact may be write as "non available"
     * git : last release does not have a link to download
     * bonita server: artefact may be not enable
     */
    protected boolean isAvailable = true;

    /**
     * multiple github source can be explode. Retains from which github this
     * apps come from
     */
    protected BonitaStore store;

    // in case of a new release exist on the store, this is the new release date
    // public Date storeReleaseDate;

    // isProvided : this page is provided by defaut on the BonitaEngine (like
    // the GroovyExample page)
    protected boolean isProvided = false;

    
    /**
     * calculate the whatsnews between the current version and the store one
     */
    protected String whatsnews;
    // add description... profile...

    protected List<BEvent> listEvents = new ArrayList<>();

    /**
     * Bonita Id (pageid for a Page, etc...), if the artefact is Deployed in a Bonita server
     */
    public BaseElement bonitaBaseElement;

    public Artifact(TypeArtifact typeArtefact, String name, String version, String description, Date dateVersion, BonitaStore store) {
        this.typeArtifact = typeArtefact;
        this.name = name.toLowerCase();
        this.version = version;
        this.description = description;
        this.dateVersion = dateVersion;
        this.store = store;

    };

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Main Attribute */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    /**
     * main attributs
     */
    /**
     * name is in lower case everytime
     */
    public void setName(final String appsName) {

        this.name = appsName == null ? "" : appsName.toLowerCase();

    }

    /**
     * name is in lower case everytime
     * 
     * @return
     */
    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDate() {
        return dateVersion;
    }

    public TypeArtifact getType() {
        return typeArtifact;
    }

    public abstract boolean isBinaryContent();

    public void addEvent(final BEvent eventMsg) {
        listEvents.add(eventMsg);
    }

    public String toString() {
        return name + " IsProvided:" + isProvided + " " + description + " URL[" + getLastUrlDownload() + "] nbRelease[" + listReleases.size() + "]";
    }

    public BonitaStore getStore() {
        return store;
    }

    public String getDisplayName() {
        return displayName == null ? getName() : displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public boolean isProvided() {
        return isProvided;
    }

    public void setProvided(boolean isProvided) {
        this.isProvided = isProvided;
    }

    
    public BaseElement getBonitaBaseElement() {
        return bonitaBaseElement;
    }

    
    public void setBonitaBaseElement(BaseElement bonitaBaseElement) {
        this.bonitaBaseElement = bonitaBaseElement;
    }

  
    
    
    public String getWhatsnews() {
        return whatsnews;
    }

    public List<BEvent> getListEvents() {
        return listEvents;
    }

    public String getUrlContent() {
        return urlContent;
    }

    public String getUrlDownload() {
        return urlDownload;
    }

    public byte[] getLogo() {
        return logo;
    }

    public List<ArtefactRelease> getListReleases() {
        return listReleases;
    }

    public void setWhatsnews(String whatsnews) {
        this.whatsnews = whatsnews;
    }

    public void setNumberOfDownload(long numberOfDownload) {
        this.numberOfDownload = numberOfDownload;
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Deploy strategy */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    private DeployStrategy deployStrategy;

    public void setDeployStrategy(DeployStrategy deployStrategy) {
        this.deployStrategy = deployStrategy;

    }

    public DeployStrategy getDeployStrategy() {
        return deployStrategy;
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Contextual information */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    /**
     * this properties are private : if some release are know, then the
     * information come the release list. Use the get() method
     * -- to be delete ?
     */
    private String lastUrlDownload;

    private Date lastReleaseDate;

    private String fileName;

    public String documentationFile;
    public String urlContent;
    public String urlDownload;
    public byte[] logo;
    private long numberOfDownload = 0;

    /**
     * contextuel information
     * 
     * @return
     */
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * getNbDownload, by summury all the download in release, or by using the
     * local information
     */
    public long getNumberOfDownload() {
        if (! listReleases.isEmpty()) {
            long total = 0;
            for (final ArtefactRelease appsRelease : listReleases) {
                total += appsRelease.numberOfDownload == null ? 0 : appsRelease.numberOfDownload;
            }
            return total;
        }
        return numberOfDownload;
    }

    public String getLastUrlDownload() {
        if (! listReleases.isEmpty()) {
            return listReleases.get(0).urlDownload;
        }
        return lastUrlDownload;
    }

    public void setLastUrlDownload(String lastUrlDownload) {
        this.lastUrlDownload = lastUrlDownload;
    }
    
    /**
     * Artifact may be loaded, or not. When it's loaded, the content is here and can be deployed
     * Remark : to load the content, it must be done via the BonitaStore
     * @return
     */
    public boolean isLoaded() {
        return bonitaBaseElement!=null;
    }
    
    /**
     * Content may be huge, and we don't need to keep in memory. So, a clean will remove all non necessary information
     * @return
     */
    public void clean() {
        bonitaBaseElement=null;
    }
    

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Content information */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    protected ByteArrayOutputStream content;

    /**
     * all Bonita artefact want to be manage as... a file :-(
     * So, whatever is the source of the artefact, it has to be saved at one moment and be ready to be
     * loaded from a file
     * 
     * @param file
     * @return
     */

    public List<BEvent> loadFromFile(File file) {
        List<BEvent> listEventsLoad = new ArrayList<>();
        try {
            content = readFile(file);
        } catch (Exception e) {
            listEventsLoad.add(new BEvent(EventReadFile, e, "Page[" + getName() + "] file[" + file.getAbsolutePath() + "]"));
        }
        return listEventsLoad;
    }

    public ByteArrayOutputStream getContent() {
        return content;
    }

    /**
     * readFile
     * 
     * @param file
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private ByteArrayOutputStream readFile(File file) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(new FileInputStream(file), out);
        return out;
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Release information */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    public class ArtefactRelease {

        public Long id;
        public String version;
        public Date dateRelease;
        public String urlDownload;
        public Long numberOfDownload;
        public String releaseNote;
    }

    public final List<ArtefactRelease> listReleases = new ArrayList<ArtefactRelease>();

    // release
    public ArtefactRelease newInstanceRelease() {
        final ArtefactRelease appsRelease = new ArtefactRelease();
        return appsRelease;
    }

    /**
     * we parse the release. For all realease AFTER the date, we complete the
     * release note.
     *
     * @param dateFrom
     * @return
     */
    public String getReleaseInformation(final Date dateFrom) {
        return "";
    }

    public Date getLastReleaseDate() {
        if (! listReleases.isEmpty()) {
            return listReleases.get(0).dateRelease;
        }
        return lastReleaseDate;
    }

    public void setLastReleaseDate(Date lastReleaseDate) {
        this.lastReleaseDate = lastReleaseDate;
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Strategy operation : deploy */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    public DeployOperation detectDeployment(BonitaStoreAccessor bonitaAccessor, LoggerStore loggerStore) {
        return deployStrategy.detectDeployment(this, bonitaAccessor, loggerStore);
    }

    public DeployOperation deploy(BonitaStoreAccessor bonitaAccessor, LoggerStore loggerStore) {
        return deployStrategy.deploy(this, bonitaAccessor, loggerStore);
    }

}
