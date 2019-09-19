package org.bonitasoft.store.artefactdeploy;

import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.artefact.Artefact;
import org.bonitasoft.store.toolbox.LoggerStore;

public class DeployStrategyRestApi extends DeployStrategyResource {

    @Override

    public DeployOperation detectDeployment(Artefact process, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        return super.detectDeployment(process, bonitaAccessor, logBox);
    }

    public DeployOperation deploy(Artefact restApi, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {

        DeployOperation deployOperation = super.deploy(restApi, bonitaAccessor, logBox);
        if (deployOperation.deploymentStatus == DeploymentStatus.DEPLOYED) {
            // in fact, no ! See https://bonitasoft.atlassian.net/browse/BS-16862
            deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
            deployOperation.listEvents.add(EventErrorAtDeployment);
        }
        return deployOperation;
    }

}
