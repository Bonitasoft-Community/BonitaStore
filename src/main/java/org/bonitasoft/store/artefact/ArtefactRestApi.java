package org.bonitasoft.store.artefact;

import java.util.Date;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStore;

public class ArtefactRestApi extends ArtefactAbstractResource {
	protected static BEvent EventDeploymentRestFailed = new BEvent(ArtefactRestApi.class.getName(), 1, Level.APPLICATIONERROR, "Rest deployment Error", "The Rest API is deployed but will failed", "The call will return a 403", "Upload manualy the restApi");

	public String name;
	public String version;
	public Date dateCreation;

	public ArtefactRestApi(String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
		super( TypeArtefact.RESTAPI, name, version, description, dateCreation, sourceOrigin);
	}

	@Override
	public String getContentType() {
		return "apiExtension";
	}

	 @Override
	  /** zip file */
	  public boolean isBinaryContent() {
	    return true;
	  }
}
