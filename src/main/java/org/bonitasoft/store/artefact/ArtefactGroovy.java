package org.bonitasoft.store.artefact;

import java.util.Date;

import org.bonitasoft.store.BonitaStore;



public class ArtefactGroovy extends ArtefactAbstractResource {


	public ArtefactGroovy(String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
		super( TypeArtefact.THEME, name, version, description, dateCreation, sourceOrigin);
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
