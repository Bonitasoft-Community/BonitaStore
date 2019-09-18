package org.bonitasoft.store.artefact;

import java.util.Date;

import org.bonitasoft.store.BonitaStore;

public class ArtefactTheme extends ArtefactAbstractResource {

  public ArtefactTheme(String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
    super(TypeArtefact.THEME, name, version, description, dateCreation, sourceOrigin);
  }

  @Override
  public String getContentType() {
    return "theme";
  }

  @Override
  /** zip file */
  public boolean isBinaryContent() {
    return true;
  }
}
