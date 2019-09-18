package org.bonitasoft.store.artefact;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveFactory;
import org.bonitasoft.engine.bpm.bar.InvalidBusinessArchiveFormatException;
import org.bonitasoft.engine.bpm.process.ActivationState;
import org.bonitasoft.engine.bpm.process.ProcessActivationException;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessDeployException;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessEnablementException;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStore;

public class ArtefactProcess extends Artefact {

  private BusinessArchive businessArchive;

  private static BEvent EventInvalidBarFile = new BEvent(ArtefactProcess.class.getName(), 1, Level.APPLICATIONERROR, "Invalid Bar file", "The bar file can't be read", "The artefact is ignored", "Check the exception");

  public ArtefactProcess(String processName, String processVersion, String processDescription, Date dateProcess, BonitaStore sourceOrigin) {
    super(TypeArtefact.PROCESS, processName, processVersion, processDescription, dateProcess, sourceOrigin);
  }

  @Override
  /** Bar file */
  public boolean isBinaryContent() {
    return true;
  }

  /**
   * load from the filefile
   * 
   * @param file
   * @throws IOException
   * @throws InvalidBusinessArchiveFormatException
   */
  @Override
  public List<BEvent> loadFromFile(File file) {
    List<BEvent> listEvents = new ArrayList<BEvent>();

    try {
      businessArchive = BusinessArchiveFactory.readBusinessArchive(file);
    } catch (Exception e) {
      listEvents.add(new BEvent(EventInvalidBarFile, e, file.getName()));
    }
    return listEvents;
  }

  public BusinessArchive getBusinessArchive() {
    return businessArchive;
  }

}
