package org.bonitasoft.store.artefact;

import java.util.Date;

import org.bonitasoft.store.BonitaStore;
import org.bonitasoft.store.artefact.Artefact.TypeArtefact;

public class ArtefactLayout extends ArtefactAbstractResource {

  public ArtefactLayout(String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
    super(TypeArtefact.LAYOUT, name, version, description, dateCreation, sourceOrigin);
  }

  @Override
  public String getContentType() {
    return "layout";
  }

  @Override
  /** zip file */
  public boolean isBinaryContent() {
    return true;
  }
}
