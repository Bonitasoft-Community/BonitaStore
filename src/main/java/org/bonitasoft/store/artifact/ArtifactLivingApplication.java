package org.bonitasoft.store.artifact;

import java.util.Date;

import org.bonitasoft.store.BonitaStore;

public class ArtifactLivingApplication extends Artifact {

    public String name;
    public String version;
    public Date dateCreation;

    public ArtifactLivingApplication(String name, String version, String description, Date dateCreation, Date dateVersion, BonitaStore sourceOrigin) {
        super(TypeArtifact.LIVINGAPP, name, version, description, dateCreation, dateVersion, sourceOrigin);
    }

    @Override
    /** zip file */
    public boolean isBinaryContent() {
        return true;
    }
}
