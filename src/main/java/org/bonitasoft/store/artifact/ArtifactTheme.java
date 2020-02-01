package org.bonitasoft.store.artifact;

import java.util.Date;

import org.bonitasoft.engine.page.Page;
import org.bonitasoft.store.BonitaStore;

public class ArtifactTheme extends ArtifactAbstractResource {

    public ArtifactTheme(String name, String version, String description, Date dateCreation, BonitaStore sourceOrigin) {
        super(TypeArtifact.THEME, name, version, description, dateCreation, sourceOrigin);
    }

    public ArtifactTheme(Page page, BonitaStore sourceOrigin) {
        super(TypeArtifact.THEME, page.getName(), "1.0", page.getDescription(), new Date(), sourceOrigin);
        setProvided( page.isProvided());
        setDisplayName(page.getDisplayName());
    }
    @Override
    public String getContentType() {
        return "theme";
    }

    @Override
    /** zip file */
    public boolean isBinaryContent() {
        return true;
    }
}
