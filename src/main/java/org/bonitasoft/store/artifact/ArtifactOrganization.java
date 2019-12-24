package org.bonitasoft.store.artefact;

import java.util.Date;

import org.bonitasoft.store.BonitaStore;

public class BArtefactOrganization extends Artefact {

    public String name;
    public String version;
    public Date dateCreation;

    public BArtefactOrganization(String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
        super(TypeArtefact.ORGANIZATION, name, version, description, dateCreation, sourceOrigin);
    }

    @Override
    /** Xml file */
    public boolean isBinaryContent() {
        return false;
    }

}
