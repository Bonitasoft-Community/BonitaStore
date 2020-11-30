package org.bonitasoft.store.artifact;

import java.io.File;
import java.util.Date;

import org.bonitasoft.engine.page.Page;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStore;

public class ArtifactRestApi extends ArtifactAbstractResource {

    protected static BEvent EventDeploymentRestFailed = new BEvent(ArtifactRestApi.class.getName(), 1, Level.APPLICATIONERROR, "Rest deployment Error", "The Rest API is deployed but will failed", "The call will return a 403", "Upload manualy the restApi");

    public String name;
    public String version;
    public Date dateCreation;

    /**
     * Create an artifact. Attention, the strategy to deploy is not referenced in the artifact via this constructor, use the FactoryArtifact for that operation
     * Default Constructor.
     * 
     * @param name
     * @param version
     * @param description
     * @param dateCreation
     * @param sourceOrigin
     */
    public ArtifactRestApi(String name, String version, String description, Date dateCreation, Date dateVersion, BonitaStore sourceOrigin) {
        super(TypeArtifact.RESTAPI, name, version, description, dateCreation, dateVersion, sourceOrigin);
    }

    public ArtifactRestApi(Page page, BonitaStore sourceOrigin) {
        super(TypeArtifact.RESTAPI, page, sourceOrigin);
    }

    public ArtifactRestApi(File file, BonitaStore sourceOrigin) {
        super(TypeArtifact.RESTAPI, file.getName(), null, "Description", new Date(file.lastModified()), new Date(file.lastModified()), sourceOrigin);
    }

    @Override
    public String getContentType() {
        return "apiExtension";
    }

    @Override
    /** zip file */
    public boolean isBinaryContent() {
        return true;
    }
}
