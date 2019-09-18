package org.bonitasoft.store.artefact;

import java.util.Date;

import org.bonitasoft.store.BonitaStore;

public class ArtefactGroovy extends ArtefactAbstractResource {

  public ArtefactGroovy(String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
    
    super(TypeArtefact.GROOVY, name, version, description, dateCreation, sourceOrigin);
    // if the name finish by a .groovy, then this is the moment to rename it
    if (name.endsWith(".groovy"))
      setName( name.substring(0, name.length() - ".groovy".length()));
    
  }

  @Override
  public String getContentType() {
    return "groovy";
  }

  @Override
  /** Text file */
  public boolean isBinaryContent() {
    return false;
  }
}
