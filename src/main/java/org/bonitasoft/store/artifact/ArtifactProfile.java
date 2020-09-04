package org.bonitasoft.store.artifact;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.engine.profile.ProfileEntry;
import org.bonitasoft.engine.profile.ProfileEntrySearchDescriptor;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStore;
import org.bonitasoft.store.BonitaStoreAccessor;

public class ArtifactProfile extends Artifact {

    private static BEvent EventErrorAtload = new BEvent(ArtifactProfile.class.getName(), 1, Level.APPLICATIONERROR, "Can't load profile file", "The profile can't be read", "profile is not accessible", "Check the exception");
    private static BEvent EventDetectionFailed = new BEvent(ArtifactProfile.class.getName(), 2, Level.ERROR, "Detection failed", "The list of profile can't be read", "profile will not be deployed", "Check the exception");
    private static BEvent EventErrorAtDeployment = new BEvent(ArtifactProfile.class.getName(), 3, Level.APPLICATIONERROR, "Can't load profile file", "The profile can't be read", "profile is not accessible", "Check the exception");

    private static BEvent EVENT_PROFILE_ENTRY_CREATED = new BEvent(ArtifactProfile.class.getName(), 4, Level.SUCCESS, "Profile entry created with success", "The required entry is created with success");
    private static BEvent EVENT_CANT_CREATE_PROFILE = new BEvent(ArtifactProfile.class.getName(), 5, Level.APPLICATIONERROR, "Can't create Profile entry", "You must use a Subscription to create a profile entry", "No entry in the profile", "Use the subscription");

    private static BEvent EVENT_FAIL_PROFILE_ENTRY_CREATION = new BEvent(ArtifactProfile.class.getName(), 6, Level.ERROR, "Can't create Profile entry", "An error arrived during the registration ", "No entry in the profile", "Check the exception");

    private static BEvent EVENT_PROFILE_ENTRY_ALREADY = new BEvent(ArtifactProfile.class.getName(), 7, Level.INFO, "Already exists", "Entry already exists", "No need to register it twice", "");

    public String name;
    public String version;
    public Date dateCreation;

    public ArtifactProfile(String profileName, String profileVersion, String description, Date dateCreation, Date dateVersion, BonitaStore sourceOrigin) {
        super(TypeArtifact.PROFILE, profileName, profileVersion, description, dateCreation, dateVersion,sourceOrigin);
        this.name = profileName;
        this.version = profileVersion;
    }

    public ArtifactProfile(Profile profile, BonitaStore sourceOrigin) {
        super(TypeArtifact.PROFILE, profile.getName(), "", profile.getDescription(), profile.getCreationDate(),profile.getLastUpdateDate(), sourceOrigin);
        setBonitaBaseElement( profile );
    }

    /**
     * load from a String. Should be a XML string (according the profile content)
     * 
     * @param contentSt
     * @return
     */
    public List<BEvent> loadFromString(String contentSt) {
        List<BEvent> listEvents = new ArrayList<BEvent>();

        byte[] profileContent = contentSt.getBytes();
        content = new ByteArrayOutputStream();
        content.write(profileContent, 0, profileContent.length);
        return listEvents;

    }

    @Override
    /** zip file */
    public boolean isBinaryContent() {
        return true;
    }

    /**
     * return the Bonita artefact
     * 
     * @return
     */
    public Profile getProfile() {
        if (bonitaBaseElement != null)
            return (Profile) bonitaBaseElement;
        return null;
    }

    /**
     * register a page in a profile.
     * Not really a deployment, more a function
     * 
     * @param page
     * @return
     */
    public List<BEvent> registerCustomPage(ArtifactCustomPage page, BonitaStoreAccessor bonitaAccessor) {
        List<BEvent> listEvents = new ArrayList<BEvent>();
        try {
            // maybe already register ?
            ProfileAPI profileAPI = bonitaAccessor.getProfileAPI();
            SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 1000);
            searchOptionsBuilder.filter(ProfileEntrySearchDescriptor.PROFILE_ID, bonitaBaseElement.getId());
            SearchResult<ProfileEntry> searchResult = profileAPI.searchProfileEntries(searchOptionsBuilder.done());
            boolean alreadyExist = false;
            for (ProfileEntry profileEntry : searchResult.getResult()) {
                if (profileEntry.getPage().equals(page.getName()))
                    alreadyExist = true;
            }
            if (alreadyExist) {

                listEvents.add(EVENT_PROFILE_ENTRY_ALREADY);
                return listEvents;
            }

            // ProfileEntry createProfileLinkEntry(String name, String description, long profileId, String page, boolean isCustom)
            Object[] params = new Object[] { page.getDisplayName(), page.getDescription(), bonitaBaseElement.getId(), page.getName(), true };
            Method[] listMethods = profileAPI.getClass().getMethods();

            // String methods="";
            for (Method method : listMethods) {
                // methods+=method.getName()+";";
                if (method.getName().equals("createProfileLinkEntry")) {
                    /*
                     * Class[] listParam = method.getParameterTypes();
                     * for (Class oneParam :listParam)
                     * methods+="("+oneParam.getName()+")";
                     */
                    method.invoke(profileAPI, params);
                    listEvents.add(EVENT_PROFILE_ENTRY_CREATED);
                    return listEvents;

                }
            }
            listEvents.add(EVENT_CANT_CREATE_PROFILE);
        } catch (Exception e) {
            listEvents.add(new BEvent(EVENT_FAIL_PROFILE_ENTRY_CREATION, e, ""));

        }
        return listEvents;
    }
}
