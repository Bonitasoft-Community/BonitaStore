package org.bonitasoft.store.artifact;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveFactory;
import org.bonitasoft.engine.bpm.bar.InvalidBusinessArchiveFormatException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStore;

public class ArtifactProcess extends Artifact {

    private BusinessArchive businessArchive;

    private static BEvent EventInvalidBarFile = new BEvent(ArtifactProcess.class.getName(), 1, Level.APPLICATIONERROR, "Invalid Bar file", "The bar file can't be read", "The artefact is ignored", "Check the exception");

    public ArtifactProcess(String processName, String processVersion, String processDescription, Date dateProcess, BonitaStore sourceOrigin) {
        super(TypeArtifact.PROCESS, processName, processVersion, processDescription, dateProcess, sourceOrigin);
    }

    @Override
    /** Bar file */
    public boolean isBinaryContent() {
        return true;
    }

    /**
     * load from the filefile
     * 
     * @param file
     * @throws IOException
     * @throws InvalidBusinessArchiveFormatException
     */
    @Override
    public List<BEvent> loadFromFile(File file) {
        List<BEvent> listEvents = new ArrayList<BEvent>();

        try {
            businessArchive = BusinessArchiveFactory.readBusinessArchive(file);
        } catch (Exception e) {
            listEvents.add(new BEvent(EventInvalidBarFile, e, file.getName()));
        }
        return listEvents;
    }

    public BusinessArchive getBusinessArchive() {
        return businessArchive;
    }

}
