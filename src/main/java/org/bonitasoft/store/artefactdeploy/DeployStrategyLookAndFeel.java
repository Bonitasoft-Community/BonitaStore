package org.bonitasoft.store.artefactdeploy;

import org.bonitasoft.store.BonitaAccessor;
import org.bonitasoft.store.artefact.Artefact;
import org.bonitasoft.store.toolbox.LoggerStore;

public class DeployStrategyLookAndFeel extends DeployStrategy{
  
  
  @Override
  public DeployOperation detectDeployment(Artefact artefact, BonitaAccessor bonitaAccessor, LoggerStore logBox) {
    DeployOperation deployOperation = new DeployOperation();
    deployOperation.detectionStatus= DetectionStatus.SAME;
    return deployOperation;
  }
  
  
  @Override
  public DeployOperation deploy(Artefact artefact, BonitaAccessor bonitaAccessor, LoggerStore logBox) {
    DeployOperation deployOperation = new DeployOperation();
    deployOperation.deploymentStatus= DeploymentStatus.DEPLOYEDFAILED;
    return deployOperation;
  }


}
