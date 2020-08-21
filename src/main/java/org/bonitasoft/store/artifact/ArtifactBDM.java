package org.bonitasoft.store.artifact;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.store.BonitaStore;

public class ArtifactBDM extends Artifact {

    public ArtifactBDM(String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
        super(TypeArtifact.BDM, name, version, description, dateCreation, sourceOrigin);
    }

    public List<BEvent> loadFromFile(File file) {
        return new ArrayList<>();
    }

    @Override
    public boolean isBinaryContent() {
        return false;
    }

   

}
