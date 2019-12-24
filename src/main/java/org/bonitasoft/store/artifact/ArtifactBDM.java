package org.bonitasoft.store.artefact;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.store.BonitaStore;

public class ArtefactBDM extends Artefact {

    public ArtefactBDM(String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
        super(TypeArtefact.BDM, name, version, description, dateCreation, sourceOrigin);
    }

    public List<BEvent> loadFromFile(File file) {
        return new ArrayList<BEvent>();
    }

    @Override
    public boolean isBinaryContent() {
        return false;
    }

}
