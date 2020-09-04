package org.bonitasoft.store.artifact;

import java.util.Date;

import org.bonitasoft.engine.page.Page;
import org.bonitasoft.store.BonitaStore;

public class ArtifactLayout extends ArtifactAbstractResource {

    public ArtifactLayout(String name, String version, String description, Date dateCreation, Date dateVersion,BonitaStore sourceOrigin) {
        super(TypeArtifact.LAYOUT, name, version, description, dateCreation,  dateVersion,sourceOrigin);
    }

    public ArtifactLayout(Page page, BonitaStore sourceOrigin) {
        super(TypeArtifact.LAYOUT, page, sourceOrigin);
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
