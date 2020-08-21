package org.bonitasoft.store.artifact;

import java.util.Date;

import org.bonitasoft.store.BonitaStore;

public abstract class ArtifactAbstractResource extends Artifact {

    // private static Logger logger = Logger.getLogger(ArtefactAbstractResource.class.getName());

    public ArtifactAbstractResource(TypeArtifact typeArtefact, String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
        super(typeArtefact, name, version, description, dateCreation, sourceOrigin);
    }

    public abstract String getContentType();

    
}
