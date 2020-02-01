package org.bonitasoft.store.artifact;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.store.BonitaStore;

public class ArtifactLookAndFeel extends Artifact {

    public String name;
    public String version;
    public Date dateCreation;

    public ArtifactLookAndFeel(String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
        super(TypeArtifact.LOOKANDFEEL, name, version, description, dateCreation, sourceOrigin);
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
