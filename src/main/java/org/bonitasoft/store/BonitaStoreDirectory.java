package org.bonitasoft.store;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStore.DetectionParameters;
import org.bonitasoft.store.BonitaStore.UrlToDownload;
import org.bonitasoft.store.artefact.Artefact;
import org.bonitasoft.store.artefact.Artefact.TypeArtefact;
import org.bonitasoft.store.artefact.ArtefactProcess;
import org.bonitasoft.store.artefact.FactoryArtefact;
import org.bonitasoft.store.artefact.FactoryArtefact.ArtefactResult;
import org.bonitasoft.store.source.git.GithubAccessor;
import org.bonitasoft.store.toolbox.LoggerStore;

public class BonitaStoreDirectory extends BonitaStore {

    private static BEvent EVENT_LOAD_FAILED = new BEvent(BonitaStoreDirectory.class.getName(), 1, Level.APPLICATIONERROR, "Error at load time", "The artefact can't be loaded", "Artefact is not accessible", "Check the exception");
    private static BEvent EVENT_CANT_MOVE_TO_ARCHIVE = new BEvent(BonitaStoreDirectory.class.getName(), 2, Level.APPLICATIONERROR, "Can't move to archived", "The artefact can't be move to the archive directory", "Artefact will be study a new time, but then mark as 'already loaded'",
            "Check the exception (access right ?)");
    private static BEvent EVENT_DIRECTORY_NOT_EXIST = new BEvent(BonitaStoreDirectory.class.getName(), 3, Level.APPLICATIONERROR, "Directory don't exist", "Bad directory name, directory don't exist (or it's not a directory?)", "No artefacts can be detected", "Check the directory name");
    private static BEvent EVENT_READ_DIRECTORY_ERROR = new BEvent(BonitaStoreDirectory.class.getName(), 4, Level.APPLICATIONERROR, "Read directory error", "Error during reading the directory", "No artefacts can be detected", "Check the directory name");

    /**
     * Directory get back from java.io
     */
    public File directoryFilePath;

    /**
     * Same name than in the HTML
     */
    private static String cstDirectory = "directory";

    @Override
    public String getName() {
        return "Dir " + directoryFilePath;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(BonitaStoreType, "Dir");
        return map;
    }

    public BonitaStoreDirectory(final File pathDirectory) {
        this.directoryFilePath = pathDirectory;
    }

    /**
     * 
     */
    @Override
    public StoreResult getListArtefacts(DetectionParameters detectionParameters, LoggerStore logBox) {
        StoreResult storeResult = new StoreResult("getListContent");
        if (directoryFilePath == null || !directoryFilePath.exists()) {
            storeResult.addEvent(new BEvent(EVENT_DIRECTORY_NOT_EXIST, "Directory[" + (directoryFilePath == null ? "null" : directoryFilePath.getAbsolutePath()) + "]"));
            return storeResult;
        }
        FactoryArtefact factoryArtefact = FactoryArtefact.getInstance();
        try {
            for (File fileContent : directoryFilePath.listFiles()) {
                if (fileContent.isDirectory())
                    continue;

                String fileName = fileContent.getName();

                String logAnalysis = "analysis[" + fileName + "]";

                ArtefactResult artefactResult = factoryArtefact.getInstanceArtefact(fileName, fileContent, this, logBox);
                // directory can contains additionnal file : no worry about that
                if (artefactResult.listEvents.size() == 1 && artefactResult.listEvents.get(0).isSameEvent(FactoryArtefact.EVENT_NO_DETECTION)) {
                    logAnalysis += "File not recognized";
                    logBox.info("ForkList.SourceDirectory " + logAnalysis);
                    continue;
                }
                storeResult.addEvents(artefactResult.listEvents);
                if (artefactResult.artefact != null) {
                    artefactResult.artefact.setFileName(fileName);

                    storeResult.listArtefacts.add(artefactResult.artefact);
                }
                logBox.info("ForkList.SourceDirectory " + logAnalysis);

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
    public StoreResult downloadArtefact(final Artefact artefactItem, UrlToDownload urlToDownload, final LoggerStore logBox) {
        // TODO Auto-generated method stub
        StoreResult storeResult = new StoreResult("downloadArtefact");
        File file = new File(directoryFilePath.getAbsolutePath() + File.separator + artefactItem.getFileName());
        if (artefactItem.isBinaryContent()) {
            try {
                storeResult.contentByte = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
            } catch (IOException e) {
                storeResult.addEvent(new BEvent(EVENT_LOAD_FAILED, e, file.getAbsolutePath()));
            }

        } else {
            storeResult.content = "";
            try (BufferedReader br = Files.newBufferedReader(Paths.get(file.getAbsolutePath()), StandardCharsets.UTF_8)) {

                // read line by line
                String line;
                while ((line = br.readLine()) != null) {
                    storeResult.content += line + "\n";
                }

            } catch (IOException e) {
                storeResult.addEvent(new BEvent(EVENT_LOAD_FAILED, e, file.getAbsolutePath()));
            }
        }
        return storeResult;
    }

    /**
     * 
     */
    @Override
    public StoreResult ping(LoggerStore logBox) {
        // check if the directory is available
        // TODO
        return new StoreResult("ping");
    }

}
