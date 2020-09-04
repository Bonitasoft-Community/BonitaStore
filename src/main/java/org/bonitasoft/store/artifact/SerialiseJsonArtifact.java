package org.bonitasoft.store.artifact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.store.BonitaStore;
import org.bonitasoft.store.BonitaStoreFactory;
import org.bonitasoft.store.artifact.Artifact.TypeArtifact;
import org.bonitasoft.store.toolbox.TypesCast;
import org.json.simple.JSONValue;

/**
 * Seriealise / Unserialise an artefact in JSON
 * 
 * @author Firstname Lastname
 */
public class SerialiseJsonArtifact {

    public static String cstJsonType = "type";
    public static String cstJsonName = "name";
    public static String cstJsonBonitaName = "bonitaname";

    public static String cstJsonVersion = "version";
    public static String cstJsonDescription = "description";
    public static String cstJsonDateCreation = "datecreation";
    public static String cstJsonStore = "store";
    
    @SuppressWarnings({ "unchecked" })
    public static Artifact getInstanceFromJsonSt(FactoryArtifact factoryArtifact, BonitaStoreFactory storeFactory, final String jsonSt) {
        if (jsonSt == null) {
            return null;
        }

        // Toolbox.logger.info("AppsItem: JsonSt[" + jsonSt + "]");
        final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);
        if (jsonHash == null) {
            return null;
        }

        String typeArtefactSt = TypesCast.getString(jsonHash.get(cstJsonType), null);
        TypeArtifact typeArtefact = typeArtefactSt == null ? null : TypeArtifact.valueOf(typeArtefactSt);

        if (typeArtefact == null)
            return null;

        String storeName = TypesCast.getString(jsonHash.get(cstJsonStore), null);
        BonitaStore store = storeFactory.getStoreByName( storeName );
        
        
        final Artifact artefact = factoryArtifact.getFromType(typeArtefact,
                TypesCast.getString(jsonHash.get(cstJsonName), null),
                TypesCast.getString(jsonHash.get(cstJsonVersion), null),
                TypesCast.getString(jsonHash.get(cstJsonDescription), null),
                TypesCast.getDate(jsonHash.get(cstJsonDateCreation),null), 
                        TypesCast.getDate(jsonHash.get(cstJsonDateCreation), null),
                store);

        String appsName = TypesCast.getString(jsonHash.get("name"), null);
        if (appsName == null) {
            return null; // does not contains a apps in fact
        }
        // appsItem.appsName = appsItem.appsName.toLowerCase();
        artefact.displayName = TypesCast.getString(jsonHash.get("displayname"), null);
        artefact.isProvided = TypesCast.getBoolean(jsonHash.get("isprovided"), false);
        artefact.setLastReleaseDate(TypesCast.getDate(jsonHash.get("installationdate"), null));
        artefact.whatsnews = TypesCast.getString(jsonHash.get("whatsnews"), null);
        artefact.setLastUrlDownload(TypesCast.getString(jsonHash.get("urldownload"), null));
        // appsItem.urlDownload = Toolbox.getString(jsonHash.get("urldownload"),
        // null);
        artefact.documentationFile = TypesCast.getString(jsonHash.get("documentationfile"), null);

        return artefact;
    }

    /**
     * get the MAP from the artefact, to create the JSON
     * 
     * @param artifact
     * @return
     */
    public static Map<String, Object> getArtifactMap(Artifact artifact) {
        final Map<String, Object> appsDetails = new HashMap<>();
        if (artifact instanceof ArtifactCustomPage) {
            List<Map<String, Object>> listProfilesMap = new ArrayList<>();
            for (ArtifactProfile profile : ((ArtifactCustomPage) artifact).getListProfiles()) {
                Map<String, Object> oneProfile = new HashMap<>();
                oneProfile.put("id", profile.getProfile()==null ? null :  profile.getProfile().getId());
                oneProfile.put("name", profile.getName());
                oneProfile.put("displayname", profile.getDisplayName());
                listProfilesMap.add(oneProfile);
            }
            appsDetails.put("listprofiles", listProfilesMap);

        }
        appsDetails.put("displayname", artifact.getDisplayName());
        appsDetails.put(cstJsonType, artifact.getType().toString());
        appsDetails.put(cstJsonName, artifact.getName());
        appsDetails.put(cstJsonBonitaName, artifact.getBonitaName());        
        appsDetails.put("description", artifact.getDescription());
        appsDetails.put("isprovided", artifact.isProvided());
        appsDetails.put("urldownload", artifact.getLastUrlDownload());
        appsDetails.put("documentationfile", artifact.documentationFile);
        appsDetails.put("showdownload", false);
        if (artifact != null && artifact.getStore() != null && artifact.getStore().isManageDownload())
            appsDetails.put("nbdownloads", artifact.getNumberOfDownload());

        if (artifact.getWhatsnews() != null) {
            appsDetails.put("whatsnews", artifact.getWhatsnews());
        }
        if ( artifact.getStore() != null) {
            appsDetails.put( cstJsonStore, artifact.getStore().getName());
        }
        // logo generated by GenerateListingItem.java
        if (artifact.getLogo() != null) {
            appsDetails.put("urllogo", "pageResource?page=custompage_foodtruck&location=storeapp/" + artifact.getName() + ".jpg");
        }
        else
            appsDetails.put("urllogo",null);

        return appsDetails;
    }


}
