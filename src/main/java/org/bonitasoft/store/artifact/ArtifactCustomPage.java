package org.bonitasoft.store.artifact;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bonitasoft.engine.page.Page;
import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.store.BonitaStore;

public class ArtifactCustomPage extends ArtifactAbstractResource {

    public String name;
    public String version;
    public String description;
    public Date dateCreation = new Date();

    public ArtifactCustomPage(String name, String version, String description, Date dateCreation, Date dateVersion, BonitaStore sourceOrigin) {
        super(TypeArtifact.CUSTOMPAGE, name, version, description, dateCreation, dateVersion, sourceOrigin);
    }

    public ArtifactCustomPage(File file, BonitaStore sourceOrigin) {
        super(TypeArtifact.CUSTOMPAGE, "name", "1.0", "Description", new Date(file.lastModified()), new Date(file.lastModified()), sourceOrigin);
    }

    public ArtifactCustomPage(Page page, BonitaStore sourceOrigin) {
        super(TypeArtifact.CUSTOMPAGE, page, sourceOrigin);
    }

    @Override
    public String getContentType() {
        return "page";
    };

    @Override
    /** zip file */
    public boolean isBinaryContent() {
        return true;
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Page are register in profile / and application */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    private List<ArtifactProfile> listProfiles = new ArrayList<ArtifactProfile>();

    public void clearProfiles() {
        listProfiles.clear();
    }

    public List<ArtifactProfile> getListProfiles() {
        return listProfiles;
    }

    public void addOneProfile(final ArtifactProfile profile) {

        for (final ArtifactProfile profileExist : listProfiles) {
            if (profileExist.getName().equals(profile.getName())) {
                return; // already register
            }
        }
        listProfiles.add(profile);
    }

    public void addOneProfile(final Profile profile) {

        for (final ArtifactProfile profileExist : listProfiles) {
            if (profileExist.getName().equals(profile.getName())) {
                return; // already register
            }
        }
        ArtifactProfile artifactProfile = new ArtifactProfile(profile, this.getStore());
        listProfiles.add(artifactProfile);
    }

}
