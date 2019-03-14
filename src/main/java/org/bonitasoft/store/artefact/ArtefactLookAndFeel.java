package org.bonitasoft.store.artefact;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.store.BonitaAccessor;
import org.bonitasoft.store.BonitaStore;
import org.bonitasoft.store.artefactdeploy.DeployStrategy.DeployOperation;
import org.bonitasoft.store.artefactdeploy.DeployStrategy.DetectionStatus;

public class ArtefactLookAndFeel extends Artefact {

	public String name;
	public String version;
	public Date dateCreation;

	public ArtefactLookAndFeel(String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
		super( TypeArtefact.LOOKANDFEEL, name, version, description, dateCreation, sourceOrigin);
	}

	

	public List<BEvent> loadFromFile(File file) {
		return new ArrayList<BEvent>();
	}

	 @Override
	  /** zip file */
	  public boolean isBinaryContent() {
	    return true;
	  }
}
