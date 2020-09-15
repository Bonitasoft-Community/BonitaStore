package org.bonitasoft.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStoreParameters.POLICY_NEWVERSION;
import org.bonitasoft.store.InputArtifact.BonitaStoreInputFile;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.ArtifactProcess;
import org.bonitasoft.store.artifact.FactoryArtifact;
import org.bonitasoft.store.artifact.FactoryArtifact.ArtifactResult;
import org.bonitasoft.store.toolbox.LoggerStore;

/**
 * BCD Target
 * BCD produce in a Target directory a set of files.
 */
public class BonitaStoreBCD extends BonitaStore {

    public static final String CST_TYPE_BCD = "BCD";

    // private final static BEvent EVENT_LOAD_FAILED = new BEvent(BonitaStoreDirectory.class.getName(), 1, Level.APPLICATIONERROR, "Error at load time", "The artefact can't be loaded", "Artefact is not accessible", "Check the exception");
    // private final static BEvent EVENT_CANT_MOVE_TO_ARCHIVE = new BEvent(BonitaStoreDirectory.class.getName(), 2, Level.APPLICATIONERROR, "Can't move to archived", "The artefact can't be move to the archive directory", "Artefact will be study a new time, but then mark as 'already loaded'",
    //              "Check the exception (access right ?)");
    private final static BEvent EVENT_DIRECTORY_NOT_EXIST = new BEvent(BonitaStoreDirectory.class.getName(), 3, Level.APPLICATIONERROR, "Directory don't exist", "Bad directory name, directory don't exist (or it's not a directory?)", "No artefacts can be detected", "Check the directory name");
    private final static BEvent EVENT_READ_DIRECTORY_ERROR = new BEvent(BonitaStoreDirectory.class.getName(), 4, Level.APPLICATIONERROR, "Read directory error", "Error during reading the directory", "No artefacts can be detected", "Check the directory name");
    private final static BEvent EVENT_READ_BCDZIP = new BEvent(BonitaStoreDirectory.class.getName(), 5, Level.APPLICATIONERROR, "Read BCD Zip error", "Error during reading the BCD Zip file", "No artefacts can be detected", "Check the directory name");

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

    public BonitaStoreBCD(final File pathDirectory) {
        this.directoryFilePath = pathDirectory;
    }

    @Override 
    public String getType() {
        return CST_TYPE_BCD;
    }

    @Override
    public void fullfillMap( Map<String,Object> map) {
        map.put("directory", directoryFilePath.getAbsolutePath());
    }

    /**
     * @param source
     * @return
     */
    public static BonitaStore getInstancefromMap(Map<String, Object> source) {
        try {
            String type = (String) source.get(CST_BONITA_STORE_TYPE);
            if (!CST_TYPE_BCD.equals(type))
                return null;
            File file = new File((String) source.get("directory"));
            BonitaStore store = new BonitaStoreBCD(file);
            store.setDisplayName((String) source.get( CST_BONITA_STORE_DISPLAYNAME));
            return store;
        } catch (Exception e) {
            return null;
        }

    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* begin and end. Some store need to do some operation before using it              */
    /* (open a connection...)                                                           */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    /**
     * Save the location where the file was unzipped
     */    
    List<Path> listZipBCDPackage = null;
    @Override
    public List<BEvent> begin(BonitaStoreParameters detectionParameters, LoggerStore logBox) {
        List<BEvent> listEvents = new ArrayList<>();
        // run every ZIP file in the BCD Directory, and unziped them
        listZipBCDPackage = new ArrayList<>();
        if (directoryFilePath == null || !directoryFilePath.exists()) {
            listEvents.add(new BEvent(EVENT_DIRECTORY_NOT_EXIST, "Directory[" + (directoryFilePath == null ? "null" : directoryFilePath.getAbsolutePath()) + "]"));
            return listEvents;
        }

        StringBuilder logAnalysis= new StringBuilder();
        for (File fileContent : directoryFilePath.listFiles()) {
            if (fileContent.isDirectory())
                continue;

            String fileName = fileContent.getName();


            // it must be a ZIP file
            if (fileName.endsWith(".zip")) {
                logAnalysis.append( "Unzip BCD[" + fileName + "]");
                try {
                Path zipRootPath = unzipFile(Paths.get(fileName), logBox);
                listZipBCDPackage.add( zipRootPath );
                }
                catch(Exception e) {
                    listEvents.add(new BEvent(EVENT_READ_BCDZIP, "BCD Zip Filename[" + fileName + "] - "+e.getMessage()));
                }
                
            }
        }
        logBox.info("BonitaStore.BCD " + logAnalysis.toString());

           return listEvents;
    }
    /**
     * purge all ZIP unzipped
     * @return
     */
    @Override
    public List<BEvent> end(BonitaStoreParameters detectionParameters, LoggerStore logBox) {
        
        for (Path path : listZipBCDPackage) {
            purgeZip(path);
        }
        return new ArrayList<>();
    }
   
    /* ******************************************************************************** */
    /*                                                                                  */
    /* Attribut */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    @Override
    public String getName() {
        return "BCD " + directoryFilePath;
    }

    public String getExplanation() {
        return "Set the BCD Target as a parameters. Then, all Build will be explode to detect artifacts to deploy";
    }

    
    public String getId() {
        return "BCD";

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
        FactoryArtifact factoryArtifact = FactoryArtifact.getInstance();

        try {
            
            for (Path bcdPackage : listZipBCDPackage) {
                
                
                File[] listArtifactsDirectory = bcdPackage.toFile().listFiles();
                for (File artifactDirectory : listArtifactsDirectory) {
                        File[] listArtifactFiles = artifactDirectory.listFiles();
                        for (File artifactFiles: listArtifactFiles) {
                            ArtifactResult artifactResult = factoryArtifact.getInstanceArtefact(artifactFiles.getName(), new BonitaStoreInputFile(artifactFiles), false, this, logBox);
                            storeResult.addEvents(artifactResult.listEvents);
                            if (artifactResult.artifact != null && detectionParameters.listTypeArtifacts.contains( artifactResult.artifact.getType())) {
                                artifactResult.artifact.setPolicyNewVersion( POLICY_NEWVERSION.NEWVERSION);
                                artifactResult.artifact.setFileName(artifactFiles.getAbsolutePath());
                                // if this is a PROCESS, then we have to deal with Parameters
                                File fileBConf = new File( directoryFilePath.toString()+"/"+bcdPackage.getFileName()+".bconf");
                                if (fileBConf.exists() && artifactResult.artifact instanceof ArtifactProcess)
                                    ((ArtifactProcess)artifactResult.artifact).setFileToCompleteTheBar( fileBConf );
                                
                                storeResult.addDetectedArtifact(detectionParameters, artifactResult);
                            }
                        }
                    }
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
        File file = (File) artifact.getSignature();
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

   

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Zip operation */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    /**
     * Unzip 
     * thanks https://mkyong.com/java/how-to-decompress-files-from-a-zip-file/
     * @param fileZip
     * @param logBox
     * @return
     * @throws IOException
     */
    private Path unzipFile(Path fileZip, final LoggerStore logBox) throws IOException {
        Path targetDir = BonitaStoreLocalServer.getTempDirectory();
        String fileZipString = fileZip.getFileName().toString();
        if (fileZipString.endsWith(".zip"))
            fileZipString = fileZipString.substring(0, fileZipString.length()-4);
                
        targetDir = Paths.get(targetDir.toAbsolutePath() + "/"+fileZipString);
        targetDir.toFile().mkdir();

        File completeZipFile = new File(directoryFilePath+"/"+fileZip.toString() );
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(  completeZipFile ))) {
            // list files in zip
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {

                boolean isDirectory = false;
                // example 1.1
                // some zip stored files and folders separately
                // e.g data/
                //     data/folder/
                //     data/folder/file.txt
                if (zipEntry.getName().endsWith(File.separator)) {
                    isDirectory = true;
                }

                Path newPath = zipSlipProtect(zipEntry, targetDir);

                if (isDirectory) {
                    Files.createDirectories(newPath);
                } else {

                    // example 1.2
                    // some zip stored file path only, need create parent directories
                    // e.g data/folder/file.txt
                    if (newPath.getParent() != null) {
                        if (Files.notExists(newPath.getParent())) {
                            Files.createDirectories(newPath.getParent());
                        }
                    }

                    // copy files, nio
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);

                    // copy files, classic
                    /*
                     * try (FileOutputStream fos = new FileOutputStream(newPath.toFile())) {
                     * byte[] buffer = new byte[1024];
                     * int len;
                     * while ((len = zis.read(buffer)) > 0) {
                     * fos.write(buffer, 0, len);
                     * }
                     * }
                     */
                }

                zipEntry = zis.getNextEntry();

            }
            zis.closeEntry();

        }
        return targetDir;
    }

    // protect zip slip attack
    public Path zipSlipProtect(ZipEntry zipEntry, Path targetDir)
            throws IOException {

        // test zip slip vulnerability
        // Path targetDirResolved = targetDir.resolve("../../" + zipEntry.getName());

        Path targetDirResolved = targetDir.resolve(zipEntry.getName());

        // make sure normalized file still has targetDir as its prefix
        // else throws exception
        Path normalizePath = targetDirResolved.normalize();
        if (!normalizePath.startsWith(targetDir)) {
            throw new IOException("Bad zip entry: " + zipEntry.getName());
        }

        return normalizePath;
    }

   
    
    private void purgeZip(Path zipPath) {
        deleteDirectory(zipPath.toFile());
    }

    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}
