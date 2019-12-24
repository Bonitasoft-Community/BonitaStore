package org.bonitasoft.store.artefactdeploy;

import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.toolbox.LoggerStore;

public class DeployStrategyBDM extends DeployStrategy {

    public DeployOperation detectDeployment(Artifact artefact, BonitaStoreAccessor bonitaAccessor, LoggerStore logger) {
        DeployOperation deployOperation = new DeployOperation();
        deployOperation.detectionStatus = DetectionStatus.SAME;
        return deployOperation;
    }

    public DeployOperation deploy(Artifact artefact, BonitaStoreAccessor bonitaAccessor, LoggerStore logger) {
        DeployOperation deployOperation = new DeployOperation();
        deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
        return deployOperation;
    }
}
