package org.bonitasoft.store.artifactdeploy;

import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.BonitaStoreParameters;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.toolbox.LoggerStore;

public class DeployStrategyRestApi extends DeployStrategyResource {

    @Override

    public DeployOperation detectDeployment(Artifact process, BonitaStoreParameters deployParameters, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        return super.detectDeployment(process, deployParameters, bonitaAccessor, logBox);
    }

    public DeployOperation deploy(Artifact restApi, BonitaStoreParameters deployParameters, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {

        DeployOperation deployOperation = super.deploy(restApi, deployParameters, bonitaAccessor, logBox);
        if (deployOperation.deploymentStatus == DeploymentStatus.DEPLOYED) {
            // in fact, no ! See https://bonitasoft.atlassian.net/browse/BS-16862
            deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
            deployOperation.listEvents.add(EventErrorAtDeployment);
        }
        return deployOperation;
    }

}
