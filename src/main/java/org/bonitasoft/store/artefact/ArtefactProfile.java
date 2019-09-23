package org.bonitasoft.store.artefact;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.bpm.BaseElement;
import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStore;

public class ArtefactProfile extends Artefact {

    private static BEvent EventErrorAtload = new BEvent(ArtefactProfile.class.getName(), 1, Level.APPLICATIONERROR, "Can't load profile file", "The profile can't be read", "profile is not accessible", "Check the exception");
    private static BEvent EventDetectionFailed = new BEvent(ArtefactProfile.class.getName(), 2, Level.ERROR, "Detection failed", "The list of profile can't be read", "profile will not be deployed", "Check the exception");
    private static BEvent EventErrorAtDeployment = new BEvent(ArtefactProfile.class.getName(), 3, Level.APPLICATIONERROR, "Can't load profile file", "The profile can't be read", "profile is not accessible", "Check the exception");

    public String name;
    public String version;
    public Date dateCreation;

    byte[] profileContent = null;

    public ArtefactProfile(String profileName, String profileVersion, String description, Date dateProfile, BonitaStore sourceOrigin) {
        super(TypeArtefact.PROFILE, profileName, profileVersion, description, dateProfile, sourceOrigin);
        this.name = profileName;
        this.version = profileVersion;
    }

    public List<BEvent> loadFromFile(File file) {
        List<BEvent> listEvents = new ArrayList<BEvent>();

        // the file is an XML, and should contains on the second line the structure
        FileInputStream fileContent = null;
        try {
            fileContent = new FileInputStream(file);
            profileContent = IOUtils.toByteArray(fileContent);
        } catch (Exception e) {
            listEvents.add(new BEvent(EventErrorAtload, e, file.getName()));
        } finally {
            if (fileContent != null)
                try {
                    fileContent.close();
                } catch (IOException e) {
                }
        }
        return listEvents;
    }

    /**
     * load from a String. Should be a XML string (according the profile content)
     * @param contentSt
     * @return
     */
    public List<BEvent> loadFromString(String contentSt)
    {
        profileContent = contentSt.getBytes();
        return new ArrayList<BEvent>();
    }
    @Override
    /** zip file */
    public boolean isBinaryContent() {
        return true;
    }
    
    /**
     * return the Bonita artefact
     * @return
     */
    public Profile getProfile()
    {
        if (bonitaBaseElement!=null)
            return (Profile) bonitaBaseElement;
        return null;
    }

}
