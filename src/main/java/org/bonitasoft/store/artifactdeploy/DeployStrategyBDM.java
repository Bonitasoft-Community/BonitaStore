package org.bonitasoft.store.artifactdeploy;

import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.BonitaStoreParameters;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.toolbox.LoggerStore;

public class DeployStrategyBDM extends DeployStrategy {

    public DeployOperation detectDeployment(Artifact artefact, BonitaStoreParameters deployParameters, BonitaStoreAccessor bonitaAccessor, LoggerStore logger) {
        DeployOperation deployOperation = new DeployOperation();
        deployOperation.detectionStatus = DetectionStatus.SAME;
        return deployOperation;
    }

    public DeployOperation deploy(Artifact artefact, BonitaStoreParameters deployParameters, BonitaStoreAccessor bonitaAccessor, LoggerStore logger) {
        DeployOperation deployOperation = new DeployOperation();
        deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
        return deployOperation;
    }
}
