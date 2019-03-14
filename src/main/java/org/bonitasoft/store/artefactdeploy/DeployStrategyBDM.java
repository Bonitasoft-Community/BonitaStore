package org.bonitasoft.store.artefactdeploy;

import org.bonitasoft.store.BonitaAccessor;
import org.bonitasoft.store.artefact.Artefact;
import org.bonitasoft.store.toolbox.LoggerStore;


public class DeployStrategyBDM extends DeployStrategy {
  
  public DeployOperation detectDeployment(Artefact artefact, BonitaAccessor bonitaAccessor, LoggerStore logger)
  {
    DeployOperation deployOperation = new DeployOperation();
    deployOperation.detectionStatus= DetectionStatus.SAME;
    return deployOperation;
  }
  
  
  
  public DeployOperation deploy(Artefact artefact, BonitaAccessor bonitaAccessor, LoggerStore logger) {
    DeployOperation deployOperation = new DeployOperation();
    deployOperation.deploymentStatus= DeploymentStatus.DEPLOYEDFAILED;
    return deployOperation;
  }
}
