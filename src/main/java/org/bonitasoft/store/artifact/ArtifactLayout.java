package org.bonitasoft.store.artifact;

import java.util.Date;

import org.bonitasoft.engine.page.Page;
import org.bonitasoft.store.BonitaStore;

public class ArtifactLayout extends ArtifactAbstractResource {

    public ArtifactLayout(String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
        super(TypeArtifact.LAYOUT, name, version, description, dateCreation, sourceOrigin);
    }

    public ArtifactLayout(Page page, BonitaStore sourceOrigin) {
        super(TypeArtifact.LAYOUT, page.getName(), "1.0", page.getDescription(), new Date(), sourceOrigin);
        setProvided( page.isProvided());
        setDisplayName(page.getDisplayName());
        setBonitaBaseElement( page );
    }
    @Override
    public String getContentType() {
        return "layout";
    }

    @Override
    /** zip file */
    public boolean isBinaryContent() {
        return true;
    }
}
