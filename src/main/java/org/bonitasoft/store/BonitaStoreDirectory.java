package org.bonitasoft.store;

import java.io.File;
import java.util.Map;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.InputArtifact.BonitaStoreInputFile;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.FactoryArtifact;
import org.bonitasoft.store.artifact.FactoryArtifact.ArtifactResult;
import org.bonitasoft.store.toolbox.LoggerStore;

/* ******************************************************************************** */
/*                                                                                  */
/* BonitaStoreDirectory */
/*                                                                                  */
/* Load the different artefact from a repository */
/*                                                                                  */
/* ******************************************************************************** */

public class BonitaStoreDirectory extends BonitaStore {

    public static final String CST_TYPE_DIR = "Dir";

    // private final static BEvent EVENT_LOAD_FAILED = new BEvent(BonitaStoreDirectory.class.getName(), 1, Level.APPLICATIONERROR, "Error at load time", "The artefact can't be loaded", "Artefact is not accessible", "Check the exception");
    private final static BEvent EVENT_DIRECTORY_NOT_EXIST = new BEvent(BonitaStoreDirectory.class.getName(), 3, Level.APPLICATIONERROR, "Directory don't exist", "Bad directory name, directory don't exist (or it's not a directory?)", "No artefacts can be detected", "Check the directory name");
    private final static BEvent EVENT_READ_DIRECTORY_ERROR = new BEvent(BonitaStoreDirectory.class.getName(), 4, Level.APPLICATIONERROR, "Read directory error", "Error during reading the directory", "No artefacts can be detected", "Check the directory name");

    /**
     * Directory get back from java.io
     */
    public File directoryFilePath;

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Creation, Serialisation,Deserialisation */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    public BonitaStoreDirectory(final File pathDirectory) {
        this.directoryFilePath = pathDirectory;
    }

    @Override
    public String getType() {
        return CST_TYPE_DIR;
    }

    @Override
    public void fullfillMap(Map<String, Object> map) {
        map.put("directory", directoryFilePath.getAbsolutePath());
    }

    /**
     * @param source
     * @return
     */
    public static BonitaStore getInstancefromMap(Map<String, Object> source) {
        try {
            String type = (String) source.get(CST_BONITA_STORE_TYPE);
            if (!CST_TYPE_DIR.equals(type))
                return null;
            File file = new File((String) source.get("directory"));
            BonitaStore store = new BonitaStoreDirectory(file);
            store.setDisplayName((String) source.get(CST_BONITA_STORE_DISPLAYNAME));
            return store;
        } catch (Exception e) {
            return null;
        }

    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Attribut */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    @Override
    public String getName() {
        return "Directory ";
    }

    @Override
    public String getExplanation() {
        return "Set the directory as parameter. Then, all Artifact present in this directory will be explode to be deployed";
    }

    public String getId() {
        return "Directory-" + directoryFilePath;

    }

    public File getDirectory() {
        return directoryFilePath;
    }

    @Override
    public boolean isManageDownload() {
        return false;
    }

    /**
     * 
     */
    @Override
    public BonitaStoreResult getListArtifacts(BonitaStoreParameters detectionParameters, LoggerStore logBox) {
        BonitaStoreResult storeResult = new BonitaStoreResult("getListContent");
        if (directoryFilePath == null || !directoryFilePath.exists()) {
            storeResult.addEvent(new BEvent(EVENT_DIRECTORY_NOT_EXIST, "Directory[" + (directoryFilePath == null ? "null" : directoryFilePath.getAbsolutePath()) + "]"));
            return storeResult;
        }
        FactoryArtifact factoryArtifact = FactoryArtifact.getInstance();
        try {
            for (File fileContent : directoryFilePath.listFiles()) {
                if (fileContent.isDirectory())
                    continue;

                String fileName = fileContent.getName();

                StringBuffer logAnalysis = new StringBuffer();

                ArtifactResult artifactResult = factoryArtifact.getInstanceArtefact(fileName, new BonitaStoreInputFile(fileContent), false, this, logBox);
                logAnalysis.append(artifactResult.logAnalysis);
                // directory can contains additional file : no worry about that
                if (artifactResult.listEvents.size() == 1 && artifactResult.listEvents.get(0).isSameEvent(FactoryArtifact.EVENT_NO_DETECTION)) {
                    logAnalysis.append("File not recognized;");
                    logBox.info("BonitaStore.SourceDirectory " + logAnalysis);
                    continue;
                }
                storeResult.addEvents(artifactResult.listEvents);
                if (artifactResult.artifact != null && detectionParameters.listTypeArtifacts.contains(artifactResult.artifact.getType())) {
                    artifactResult.artifact.setFileName(fileName);

                    storeResult.addDetectedArtifact(detectionParameters, artifactResult);
                }
                logBox.info("BonitaStore.SourceDirectory " + logAnalysis.toString());

            }
        } catch (Exception e) {
            logBox.info("SourceDirectory.getListArtefactDetected Exception [" + e.toString() + "]");
            storeResult.addEvent(new BEvent(EVENT_READ_DIRECTORY_ERROR, "Directory[" + (directoryFilePath == null ? "null" : directoryFilePath.getAbsolutePath()) + "]"));

        }
        return storeResult;
    }

    /**
     * 
     */
    @Override
    public BonitaStoreResult loadArtifact(final Artifact artifact, UrlToDownload urlToDownload, final LoggerStore logBox) {
        BonitaStoreResult storeResult = new BonitaStoreResult("load");
        File file = new File(directoryFilePath.getAbsolutePath() + File.separator + artifact.getFileName());
        storeResult.addEvents(artifact.loadFromFile(file));
        return storeResult;
    }

    /**
     * 
     */
    @Override
    public BonitaStoreResult ping(LoggerStore logBox) {
        BonitaStoreResult bonitaStoreResult = new BonitaStoreResult("ping");
        // check if the directory is available
        if (!directoryFilePath.isDirectory())
            bonitaStoreResult.addEvent(new BEvent(EVENT_DIRECTORY_NOT_EXIST, "Directory [" + directoryFilePath.getAbsolutePath() + "]"));

        return new BonitaStoreResult("ping");
    }

}
