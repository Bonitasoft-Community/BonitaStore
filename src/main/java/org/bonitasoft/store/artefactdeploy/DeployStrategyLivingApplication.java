package org.bonitasoft.store.artefactdeploy;

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
import org.bonitasoft.store.BonitaAccessor;
import org.bonitasoft.store.artefact.Artefact;
import org.bonitasoft.store.toolbox.LoggerStore;

public class DeployStrategyLivingApplication extends DeployStrategy {

  @Override
  public DeployOperation detectDeployment(Artefact artefact, BonitaAccessor bonitaAccessor, LoggerStore logBox) {
    DeployOperation deployOperation = new DeployOperation();
    try {
      Application application = searchByName(artefact, bonitaAccessor);
      if (application != null) {
        deployOperation.presentDateArtefact = application.getLastUpdateDate();
        deployOperation.presentVersionArtefact = null;
      }
    } catch (SearchException e) {
      deployOperation.detectionStatus = DetectionStatus.DETECTIONFAILED;
      deployOperation.listEvents.add(new BEvent(EventErrorAtDetection, e, "Application [" + artefact.getName() + "]"));
    }
    return deployOperation;
  }

  @Override
  public DeployOperation deploy(Artefact artefact, BonitaAccessor bonitaAccessor, LoggerStore logBox) {
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

  public Application searchByName(Artefact artefact, BonitaAccessor bonitaAccessor) throws SearchException {
    SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 10);
    searchOptionsBuilder.filter(ApplicationSearchDescriptor.DISPLAY_NAME, artefact.getName());
    SearchResult<Application> searchResultApplication = bonitaAccessor.applicationAPI.searchApplications(searchOptionsBuilder.done());
    for (final Application application : searchResultApplication.getResult())
      return application;
    return null;
  }
}
