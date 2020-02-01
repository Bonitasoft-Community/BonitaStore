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

    public ArtifactRestApi(String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
        super(TypeArtifact.RESTAPI, name, version, description, dateCreation, sourceOrigin);
    }
    public ArtifactRestApi(Page page, BonitaStore sourceOrigin) {
        super(TypeArtifact.RESTAPI, page.getName(), "1.0", page.getDescription(), new Date(), sourceOrigin);
        setProvided( page.isProvided());
        setDisplayName(page.getDisplayName());
        setBonitaBaseElement( page );
    }
    public ArtifactRestApi(File fileName, BonitaStore sourceOrigin) {
        super(TypeArtifact.RESTAPI, "name", "1.0", "Description", new Date(), sourceOrigin);
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
