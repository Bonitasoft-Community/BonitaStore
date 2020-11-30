package org.bonitasoft.store.artifact;

import java.util.Date;

import org.bonitasoft.store.BonitaStore;

public class ArtifactGroovy extends ArtifactAbstractResource {

    public ArtifactGroovy(String name, String version, String description, Date dateCreation, Date dateVersion, BonitaStore sourceOrigin) {

        super(TypeArtifact.GROOVY, name, version, description, dateCreation, dateVersion, sourceOrigin);
        // if the name finish by a .groovy, then this is the moment to rename it
        if (name.endsWith(".groovy"))
            setName(name.substring(0, name.length() - ".groovy".length()));

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
