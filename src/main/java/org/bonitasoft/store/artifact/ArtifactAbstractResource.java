package org.bonitasoft.store.artefact;

import java.util.Date;

import org.bonitasoft.store.BonitaStore;

public abstract class ArtefactAbstractResource extends Artefact {

    // private static Logger logger = Logger.getLogger(ArtefactAbstractResource.class.getName());

 
    public ArtefactAbstractResource(TypeArtefact typeArtefact, String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
        super(typeArtefact, name, version, description, dateCreation, sourceOrigin);
    }

   
    public abstract String getContentType();

}
