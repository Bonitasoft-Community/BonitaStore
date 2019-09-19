package org.bonitasoft.store.artefactdeploy;

import java.lang.reflect.Method;

import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.artefact.Artefact;
import org.bonitasoft.store.toolbox.LoggerStore;

public class DeployStrategyProfile extends DeployStrategy {

    @Override
    public DeployOperation detectDeployment(Artefact artefactProfile, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        DeployOperation deployOperation = new DeployOperation();
        try {
            SearchResult<Profile> searchProfile = bonitaAccessor.profileAPI.searchProfiles(new SearchOptionsBuilder(0, 1000).done());
            for (Profile profile : searchProfile.getResult()) {
                if (profile.getName().equals(artefactProfile.getName())) {
                    deployOperation.presentDateArtefact = profile.getLastUpdateDate();

                    if (profile.getLastUpdateDate().equals(artefactProfile.getDate())) {
                        deployOperation.detectionStatus = DetectionStatus.SAME;
                        deployOperation.report = "the profile exist with the same date (" + sdf.format(artefactProfile.getDate()) + ")";

                    } else if (profile.getLastUpdateDate().before(artefactProfile.getDate())) {
                        deployOperation.detectionStatus = DetectionStatus.NEWVERSION;
                        deployOperation.report = "The profile has a newest date";

                    } else {
                        deployOperation.detectionStatus = DetectionStatus.OLDVERSION;
                        deployOperation.report = "The profile on the server is newest";
                    }
                    return deployOperation;
                }
            }
            deployOperation.report = "This profile is new";
            deployOperation.detectionStatus = DetectionStatus.NEWARTEFAC;
        } catch (Exception e) {
            deployOperation.listEvents.add(new BEvent(EventErrorAtDetection, e, "Profile name[" + artefactProfile.getName() + "]"));
            deployOperation.detectionStatus = DetectionStatus.DETECTIONFAILED;
        }
        return deployOperation;
    }

    @Override
    public DeployOperation deploy(Artefact artefactProfile, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {

        DeployOperation deployOperation = new DeployOperation();
        deployOperation.deploymentStatus = DeploymentStatus.NOTHINGDONE;

        // game here : we just include the ORG import, but this fonction is a COM function
        try {
            // ok, please give me the correct profileAPI (expected the com here)
            ProfileAPI profileAPI = bonitaAccessor.getProfileAPI();

            // get the policy : com.bonitasoft.engine.profile.ImportPolicy.REPLACE_DUPLICATES;
            Class<?> clImportPolicy = Class.forName("com.bonitasoft.engine.profile.ImportPolicy");
            if (clImportPolicy == null) {
                deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, "Profile name[" + artefactProfile.getName() + "] - Only Subscription edition"));
                deployOperation.deploymentStatus = DeploymentStatus.BADBONITAVERSION;
                return deployOperation;
            }
            Object[] params = new Object[] { String.class };
            Method methodValueOf = clImportPolicy.getMethod("valueOf", String.class);
            Object importPolicy = methodValueOf.invoke(null, "REPLACE_DUPLICATES");

            params = new Object[] { artefactProfile.getContent(), importPolicy };

            // this methode can't be join..
            // java.lang.reflect.Method method = profileAPI.getClass().getMethod("importProfiles", byte[].class, clImportPolicy.getClass() );
            // so let search it
            Method[] listMethods = profileAPI.getClass().getMethods();

            // String methods="";
            for (Method method : listMethods) {
                // methods+=method.getName()+";";
                if (method.getName().equals("importProfiles")) {
                    /*
                     * Class[] listParam = method.getParameterTypes();
                     * for (Class oneParam :listParam)
                     * methods+="("+oneParam.getName()+")";
                     */
                    method.invoke(profileAPI, params);
                    deployOperation.deploymentStatus = DeploymentStatus.DEPLOYED;
                }
            }

            if (deployOperation.deploymentStatus == DeploymentStatus.NOTHINGDONE) {
                deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, "Profile name[" + artefactProfile.getName() + "] - Only Subscription edition"));
                deployOperation.deploymentStatus = DeploymentStatus.BADBONITAVERSION;

            }
        } catch (Exception e) {
            deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, e, "Profile name[" + artefactProfile.getName() + "]"));
            deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
        }

        return deployOperation;
    }
}
