package org.bonitasoft.store.artefact;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.bonitasoft.engine.page.Page;
import org.bonitasoft.engine.page.PageNotFoundException;
import org.bonitasoft.engine.page.PageSearchDescriptor;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStore;
import org.bonitasoft.store.artefactdeploy.DeployStrategy;

public abstract class ArtefactAbstractResource extends Artefact {

  private static Logger logger = Logger.getLogger(ArtefactAbstractResource.class.getName());

  protected static BEvent EventReadFile = new BEvent(ArtefactAbstractResource.class.getName(), 1, Level.APPLICATIONERROR, "File error", "The file can't be read", "The artefact is ignored", "Check the exception");

  public ArtefactAbstractResource(TypeArtefact typeArtefact, String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
    super(typeArtefact, name, version, description, dateCreation, sourceOrigin);
  }

  protected ByteArrayOutputStream content;

  public List<BEvent> loadFromFile(File file) {
    List<BEvent> listEvents = new ArrayList<BEvent>();
    try {
      content = readFile(file);
    } catch (Exception e) {
      listEvents.add(new BEvent(EventReadFile, e, "Page[" + getName() + "] file[" + file.getAbsolutePath() + "]"));
    }
    return listEvents;
  }

  public abstract String getContentType();

  public ByteArrayOutputStream getContent() {
    return content;

  }

  protected ByteArrayOutputStream readFile(File file) throws FileNotFoundException, IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IOUtils.copy(new FileInputStream(file), out);
    return out;
  }
}
