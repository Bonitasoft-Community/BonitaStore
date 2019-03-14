package org.bonitasoft.store.artefact;

import java.util.Date;


import org.bonitasoft.store.BonitaStore;



public class ArtefactCustomPage extends ArtefactAbstractResource {


	public String name;
	public String version;
	public String description;
	public Date dateCreation = new Date();

	
	public ArtefactCustomPage(String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
		super( TypeArtefact.CUSTOMPAGE, name, version, description, dateCreation, sourceOrigin);
	}

	
	@Override
	public String getContentType()
	{ return "page"; };
	
  @Override
  /** zip file */
  public boolean isBinaryContent() {
    return true;
  }


}
