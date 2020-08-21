package org.bonitasoft.store.artifactdeploy;

import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.toolbox.LoggerStore;

public class DeployStrategyLookAndFeel extends DeployStrategy {

    @Override
    public DeployOperation detectDeployment(Artifact artefact, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        DeployOperation deployOperation = new DeployOperation();
        deployOperation.detectionStatus = DetectionStatus.SAME;
        return deployOperation;
    }

    @Override
    public DeployOperation deploy(Artifact artefact, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        DeployOperation deployOperation = new DeployOperation();
        deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
        return deployOperation;
    }

}
