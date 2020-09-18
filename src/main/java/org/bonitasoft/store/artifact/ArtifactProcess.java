package org.bonitasoft.store.artifact;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveFactory;
import org.bonitasoft.engine.bpm.bar.InvalidBusinessArchiveFormatException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStore;
import org.bonitasoft.store.BonitaStoreLocalServer;

public class ArtifactProcess extends Artifact {

    private BusinessArchive businessArchive;

    private final static BEvent EventInvalidBarFile = new BEvent(ArtifactProcess.class.getName(), 1, Level.APPLICATIONERROR, "Invalid Bar file", "The bar file can't be read", "The artefact is ignored", "Check the exception");

    public ArtifactProcess(String processName, String processVersion, String processDescription, Date dateProcess, Date dateVersion, BonitaStore sourceOrigin) {
        super(TypeArtifact.PROCESS, processName, processVersion, processDescription, dateProcess, dateVersion,sourceOrigin);
    }

    @Override
    /** Bar file */
    public boolean isBinaryContent() {
        return true;
    }

    /**
     * load from the file
     * 
     * @param file
     * @throws IOException
     * @throws InvalidBusinessArchiveFormatException
     */
    @Override
    public List<BEvent> loadFromFile(File file) {
        List<BEvent> listEvents = new ArrayList<>();

        try {
            InputStream fileInput;
            File temporaryFileName = null;
            // if the completeFile is given, then do the merge
            if (configurationFileToReplace != null) {
                temporaryFileName = merge(file);
                fileInput = new FileInputStream(temporaryFileName);
            } else
                fileInput = new FileInputStream(file);

            businessArchive = BusinessArchiveFactory.readBusinessArchive(fileInput);

            if (temporaryFileName != null) {
                temporaryFileName.delete();
            }
        } catch (Exception e) {
            listEvents.add(new BEvent(EventInvalidBarFile, e, file.getName()));
        }
        return listEvents;
    }

    public BusinessArchive getBusinessArchive() {
        return businessArchive;
    }

    @Override
    public boolean isLoaded() {
        return businessArchive != null;
    }

    @Override
    public void clean() {
        businessArchive = null;
    }

    /**
     * Some tool, like BCD, maintains information OUTSIDE the bar file, in order to merge them. Give this file here
     * 
     * @param inputFile
     */
    private File configurationFileToReplace = null;

    public void setFileToCompleteTheBar(File inputFile) {
        this.configurationFileToReplace = inputFile;
    }

    /**
     * @param sourceFile
     * @return
     * @throws FileNotFoundException
     */
    public File merge(File sourceFile) throws FileNotFoundException {

        Path path = BonitaStoreLocalServer.getTempDirectory();
        // generate a temporary file
        File temporaryFile = new File(path.toString() + "/" + getBonitaName() + ".bar");


        try (FileOutputStream fos = new FileOutputStream(temporaryFile);ZipOutputStream zipOut = new ZipOutputStream(fos)){
            // open a Zip file here
            
            

            // we explore the source file to collect all the replacement file
            Set<String> replacementFile = exploreZipToExploreAndCompleteReplacement(configurationFileToReplace,null);
            
            // we read the current source File
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceFile))) {
                // list files in zip
                ZipEntry zipInEntry = zis.getNextEntry();

                while (zipInEntry != null) {

                    if (zipInEntry.getName().endsWith(File.separator)) {
                        zipOut.putNextEntry(new ZipEntry(zipInEntry.getName()));
                        zipOut.closeEntry();
                    } else {

                        if (replacementFile.contains(zipInEntry.getName())) {

                        } else {
                            // copy

                            ZipEntry zipOutEntry = new ZipEntry(zipInEntry.getName());
                            zipOut.putNextEntry(zipOutEntry);
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                zipOut.write(buffer, 0, len);
                            }
                        }
                    }
                    zipInEntry = zis.getNextEntry();
                }
                
                // now, complete the zipOut
                exploreZipToExploreAndCompleteReplacement(configurationFileToReplace,zipOut);

                
                zis.closeEntry();
                fos.flush();

            }
        } catch (Exception e) {
            return null;
        }
        traceZip(sourceFile);
        traceZip(temporaryFile);

        return temporaryFile;
    }

    /**
     * 
     * @param sourceFile
     * @param zipOut id not null, then the zip is populated with this information
     * @return
     */
    private Set<String> exploreZipToExploreAndCompleteReplacement(File sourceFile,ZipOutputStream zipOut ) {
        Set<String> replacementFile = new HashSet<>();
        traceZip(sourceFile);
        String filterEntry = (getBonitaName()+"/"+getVersion()).toLowerCase()+"/";
        try (FileInputStream fis = new FileInputStream(sourceFile); ZipInputStream zis = new ZipInputStream(fis)) {
            // list files in zip
            ZipEntry zipInEntry = zis.getNextEntry();

            while (zipInEntry != null) {
                if (zipInEntry.getName().endsWith(File.separator)) {
                } else {
                    if (zipInEntry.getName().toLowerCase().startsWith( filterEntry)) {
                        String fileNameReplacement =zipInEntry.getName().substring(filterEntry.length()); 
                        replacementFile.add( fileNameReplacement );
                        
                        // copy
                        if (zipOut != null) {
                            ZipEntry zipOutEntry = new ZipEntry( fileNameReplacement );
                            zipOut.putNextEntry(zipOutEntry);
    
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                zipOut.write(buffer, 0, len);
                            }
                        }
                    }
                }
                zipInEntry = zis.getNextEntry();

            }
            zis.closeEntry();

        } catch (Exception e) {
        }
        return replacementFile;
    }
    private String traceZip(File sourceFile) {
        StringBuffer traceResult = new StringBuffer();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceFile))) {
            // list files in zip
            ZipEntry zipInEntry = zis.getNextEntry();

            while (zipInEntry != null) {
                traceResult.append("[" + zipInEntry.getName() + "] ");
                if (zipInEntry.getName().endsWith(File.separator)) {
                    traceResult.append("Directory");
                } else {
                    // copy
                    int lenEntry = 0;
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        lenEntry += len;
                    }

                    traceResult.append("File " + lenEntry);
                }
                zipInEntry = zis.getNextEntry();

            }
            zis.closeEntry();

            
        } catch (Exception e) {
            traceResult.append("Exception " + e.toString());
        }
        return traceResult.toString();
    }
    /*
     * private void unzip() {
     * String fileZip = "src/main/resources/unzipTest/compressed.zip";
     * File destDir = new File("src/main/resources/unzipTest");
     * byte[] buffer = new byte[1024];
     * ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
     * ZipEntry zipInEntry = zis.getNextEntry();
     * while (zipInEntry != null) {
     * File newFile = newFile(destDir, zipInEntry);
     * FileOutputStream fos = new FileOutputStream(newFile);
     * int len;
     * while ((len = zis.read(buffer)) > 0) {
     * fos.write(buffer, 0, len);
     * }
     * fos.close();
     * zipInEntry = zis.getNextEntry();
     * }
     * zis.closeEntry();
     * zis.close();
     * }
     */
}
