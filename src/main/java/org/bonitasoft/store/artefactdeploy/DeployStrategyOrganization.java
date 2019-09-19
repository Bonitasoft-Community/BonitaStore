package org.bonitasoft.store.artefactdeploy;

import org.bonitasoft.engine.identity.ImportPolicy;
import org.bonitasoft.engine.identity.OrganizationImportException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.artefact.Artefact;
import org.bonitasoft.store.toolbox.LoggerStore;

public class DeployStrategyOrganization extends DeployStrategy {

    @Override
    public DeployOperation detectDeployment(Artefact artefact, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        DeployOperation deployOperation = new DeployOperation();
        deployOperation.detectionStatus = DetectionStatus.NEWARTEFAC;
        return deployOperation;
    }

    @Override
    public DeployOperation deploy(Artefact artefact, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        DeployOperation deployOperation = new DeployOperation();
        try {
            bonitaAccessor.organisationAPI.importOrganization(artefact.getContent().toString(), ImportPolicy.MERGE_DUPLICATES);
        } catch (OrganizationImportException e) {
            deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, e, ""));
            logBox.severe("Forklift.ArtefactOrganization  error import organization " + e.toString());
        }
        deployOperation.deploymentStatus = DeploymentStatus.DEPLOYED;
        return deployOperation;
    }

}
