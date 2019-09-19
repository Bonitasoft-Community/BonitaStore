package org.bonitasoft.store.artefact;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.bonitasoft.engine.business.application.Application;
import org.bonitasoft.engine.business.application.ApplicationImportPolicy;
import org.bonitasoft.engine.business.application.ApplicationSearchDescriptor;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ImportException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.store.BonitaStore;

public class ArtefactLivingApplication extends Artefact {

    public String name;
    public String version;
    public Date dateCreation;

    public ArtefactLivingApplication(String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
        super(TypeArtefact.LIVINGAPP, name, version, description, dateCreation, sourceOrigin);
    }

    @Override
    /** zip file */
    public boolean isBinaryContent() {
        return true;
    }
}
