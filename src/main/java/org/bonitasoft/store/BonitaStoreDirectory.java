package org.bonitasoft.store;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
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
import org.bonitasoft.store.toolbox.LoggerStore;

public class BonitaStoreDirectory extends BonitaStore {
  
  private static BEvent EventLoadFailed = new BEvent(BonitaStoreDirectory.class.getName(), 1, Level.APPLICATIONERROR, "Error at load time", "The artefact can't be loaded", "Artefact is not accessible", "Check the exception");
  private static BEvent EventCantMoveToArchive = new BEvent(BonitaStoreDirectory.class.getName(), 2, Level.APPLICATIONERROR, "Can't move to archived", "The artefact can't be move to the archive directory", "Artefact will be study a new time, but then mark as 'already loaded'",
      "Check the exception (access right ?)");

  /**
   * directory given by administrator
   */
  public String directory;
  /**
   * Directory get back from java.io
   */
  public String directoryFilePath;

  /**
   * Same name than in the HTML
   */
  private static String cstDirectory = "directory";


  @Override
  public String getName() {
   return "Dir "+directoryFilePath;
  }

  @Override
  public Map<String,Object> toMap()
  {
    Map<String,Object> map = new HashMap<String,Object>();
    map.put( BonitaStoreType,  "Dir");
    return map;
  }

  @Override
  public StoreResult getListArtefacts(DetectionParameters detectionParameters, LoggerStore logBox) {
    StoreResult storeResult = new StoreResult("getListContent");
    
    try {
      File dirMonitor = new File(directory);
      this.directoryFilePath = dirMonitor.getAbsolutePath();
      for (File fileContent : dirMonitor.listFiles()) {
        if (fileContent.isDirectory())
          continue;
       

        
        String fileName = fileContent.getName();

        String logAnalysis = "analysis[" + fileName + "]";
        BasicFileAttributes attributes = Files.readAttributes(fileContent.toPath(), BasicFileAttributes.class);

        Date dateFile = new Date(attributes.lastModifiedTime().toMillis());

       FactoryArtefact factoryArtefact= FactoryArtefact.getInstance();
       ArtefactResult artefactResult = factoryArtefact.getInstanceArtefact(fileName, fileContent, this, logBox);
       storeResult.addEvents(  artefactResult.listevents);
        if (artefactResult.artefact != null) {
          artefactResult.artefact.setFileName( fileName );
          artefactResult.artefact.sourceBonitaStore = this;

          storeResult.listArtefacts.add( artefactResult.artefact );
        }
        logBox.info("ForkList.SourceDirectory " + logAnalysis);

      }
    } catch (Exception e) {
      logBox.info("SourceDirectory.getListArtefactDetected Exception [" + e.toString() + "]");

    }
    return storeResult;
  }

  @Override
  public StoreResult downloadArtefact(final Artefact artefactItem, UrlToDownload urlToDownload,  final LoggerStore logBox) {
    // TODO Auto-generated method stub
    return null;
  }

}
