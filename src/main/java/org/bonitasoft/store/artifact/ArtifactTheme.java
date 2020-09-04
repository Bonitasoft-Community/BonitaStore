package org.bonitasoft.store.artifact;

import java.util.Date;

import org.bonitasoft.engine.page.Page;
import org.bonitasoft.store.BonitaStore;

public class ArtifactTheme extends ArtifactAbstractResource {

    public ArtifactTheme(String name, String version, String description, Date dateCreation, Date dateVersion, BonitaStore sourceOrigin) {
        super(TypeArtifact.THEME, name, version, description, dateCreation,  dateVersion, sourceOrigin);
    }

    public ArtifactTheme(Page page, BonitaStore sourceOrigin) {
        super(TypeArtifact.THEME, page, sourceOrigin);
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
