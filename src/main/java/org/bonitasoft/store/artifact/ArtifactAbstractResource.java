package org.bonitasoft.store.artifact;

import java.util.Date;

import org.bonitasoft.engine.page.Page;
import org.bonitasoft.store.BonitaStore;

public abstract class ArtifactAbstractResource extends Artifact {

    // private static Logger logger = Logger.getLogger(ArtefactAbstractResource.class.getName());

    public ArtifactAbstractResource(TypeArtifact typeArtefact, String name, String version, String description, Date dateCreation, Date dateVersion, BonitaStore sourceOrigin) {
        super(typeArtefact, name, version, description, dateCreation, dateVersion, sourceOrigin);
    }
    public ArtifactAbstractResource(TypeArtifact typeArtefact, Page page, BonitaStore sourceOrigin) {
        super(typeArtefact, page.getName(), null, page.getDescription(), page.getLastModificationDate(),  page.getLastModificationDate(), sourceOrigin);
        setProvided( page.isProvided());
        setDisplayName(page.getDisplayName());
        setBonitaBaseElement( page );

    }
    public abstract String getContentType();

    
}
