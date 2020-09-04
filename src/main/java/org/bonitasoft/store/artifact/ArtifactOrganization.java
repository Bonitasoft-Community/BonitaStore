package org.bonitasoft.store.artifact;

import java.util.Date;

import org.bonitasoft.store.BonitaStore;

public class ArtifactOrganization extends Artifact {

    public String name;
    public String version;
    public Date dateCreation;

    public ArtifactOrganization(String name, String version, String description, Date dateCreation, Date dateVersion, BonitaStore sourceOrigin) {
        super(TypeArtifact.ORGANIZATION, name, version, description, dateCreation, dateVersion,sourceOrigin);
    }

    @Override
    /** Xml file */
    public boolean isBinaryContent() {
        return false;
    }

}
