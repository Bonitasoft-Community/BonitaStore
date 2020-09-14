package org.bonitasoft.store.artifactdeploy;

import org.bonitasoft.engine.business.application.Application;
import org.bonitasoft.engine.business.application.ApplicationImportPolicy;
import org.bonitasoft.engine.business.application.ApplicationSearchDescriptor;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ImportException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.BonitaStoreParameters;
import org.bonitasoft.store.BonitaStoreParameters.POLICY_NEWVERSION;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifactdeploy.DeployStrategy.DetectionStatus;
import org.bonitasoft.store.toolbox.LoggerStore;

public class DeployStrategyLivingApplication extends DeployStrategy {

    @Override
    public DeployOperation detectDeployment(Artifact artifact, BonitaStoreParameters deployParameters, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        DeployOperation deployOperation = new DeployOperation();
        deployOperation.detectionStatus = DetectionStatus.NEWARTEFAC;

        try {
            Application application = searchByName(artifact, bonitaAccessor);
            if (application != null) {
                deployOperation.presentDateArtifact = application.getLastUpdateDate();
                deployOperation.presentVersionArtifact = "";
                if (POLICY_NEWVERSION.BYDATE.equals(artifact.getPolicyNewVersion(deployParameters.policyNewVersion))) {
                    if (artifact.getLastReleaseDate().before(application.getLastUpdateDate())) {
                        deployOperation.detectionStatus = DetectionStatus.SAME; // or OLD...
                        deployOperation.addAnalysisLine( "A version exists with the date more recent(" + DeployStrategy.sdf.format(application.getLastUpdateDate()) + ")");
                    } else {
                        deployOperation.detectionStatus = DetectionStatus.NEWVERSION;
                        deployOperation.addAnalysisLine( "The version is new");
                    }
                } else {
                    // well, no way to know
                    deployOperation.detectionStatus = DetectionStatus.UNDETERMINED;
                }
            }
        } catch (SearchException e) {
            deployOperation.detectionStatus = DetectionStatus.DETECTIONFAILED;
            deployOperation.listEvents.add(new BEvent(EventErrorAtDetection, e, "Application [" + artifact.getName() + "]"));
        }
        return deployOperation;
    }

    @Override
    public DeployOperation deploy(Artifact artefact, BonitaStoreParameters deployParameters, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        DeployOperation deployOperation = new DeployOperation();

        try {
            Application application = searchByName(artefact, bonitaAccessor);
            if (application != null)
                bonitaAccessor.applicationAPI.deleteApplication(application.getId());

            bonitaAccessor.applicationAPI.importApplications(artefact.getContent().toByteArray(), ApplicationImportPolicy.FAIL_ON_DUPLICATES);
            deployOperation.deploymentStatus = DeploymentStatus.DEPLOYED;
        } catch (AlreadyExistsException e) {
            deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, e, "Application " + artefact.getName()));
            deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
            logBox.severe("Forklift.ArtefactLivingApplication Error during deployement " + artefact.getName() + " : " + e.toString());
        } catch (ImportException e) {
            deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, e, "Application " + artefact.getName()));
            deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
            logBox.severe("Forklift.ArtefactLivingApplication Error during deployement " + artefact.getName() + " : " + e.toString());
        } catch (DeletionException e) {
            deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, e, "Application " + artefact.getName()));
            deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
            logBox.severe("Forklift.ArtefactLivingApplication Error during deployement " + artefact.getName() + " : " + e.toString());
        } catch (SearchException e) {
            deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, e, "Application " + artefact.getName()));
            deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
            logBox.severe("Forklift.ArtefactLivingApplication Error during deployement " + artefact.getName() + " : " + e.toString());
        }

        return deployOperation;
    }

    public Application searchByName(Artifact artefact, BonitaStoreAccessor bonitaAccessor) throws SearchException {
        SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 10);
        searchOptionsBuilder.filter(ApplicationSearchDescriptor.DISPLAY_NAME, artefact.getName());
        SearchResult<Application> searchResultApplication = bonitaAccessor.applicationAPI.searchApplications(searchOptionsBuilder.done());
        for (final Application application : searchResultApplication.getResult())
            return application;
        return null;
    }
}
