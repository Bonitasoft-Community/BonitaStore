package org.bonitasoft.store.artefactdeploy;

import org.bonitasoft.engine.bpm.process.ActivationState;
import org.bonitasoft.engine.bpm.process.ProcessActivationException;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessDeployException;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessEnablementException;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaAccessor;
import org.bonitasoft.store.artefact.Artefact;
import org.bonitasoft.store.artefact.ArtefactProcess;
import org.bonitasoft.store.toolbox.LoggerStore;

public class DeployStrategyProcess extends DeployStrategy {

  protected static BEvent EventCantRemoveCurrentProcess = new BEvent(DeployStrategyProcess.class.getName(), 1, Level.APPLICATIONERROR, "Current process can't be removed", "To deploy the new process (same name, same version), current process has to be removed. The operation failed.",
      "Deployment of the new process is not possible", "Check the exception");

  /* *********************************************************************** */
  /*                                                                         */
  /* Deployement */
  /*                                                                         */
  /*                                                                         */
  /* *********************************************************************** */

  public DeployOperation detectDeployment(Artefact process, BonitaAccessor bonitaAccessor, LoggerStore logBox) {
    DeployOperation deployOperation = new DeployOperation();
    Long processDefinitionId = null;
    try {
      processDefinitionId = bonitaAccessor.processAPI.getProcessDefinitionId(process.getName(), process.getVersion());

    } catch (ProcessDefinitionNotFoundException pe) {
    }

    if (processDefinitionId != null) {
      try {
        ProcessDeploymentInfo processDeploymentInfo = bonitaAccessor.processAPI.getProcessDeploymentInfo(processDefinitionId);
        deployOperation.presentDateArtefact = processDeploymentInfo.getDeploymentDate();
        deployOperation.presentVersionArtefact = process.getVersion();
        if (processDeploymentInfo.getDeploymentDate().equals(process.getDate())) {
          deployOperation.detectionStatus = DetectionStatus.SAME;
          deployOperation.report = "A version exist with the same date (" + DeployStrategy.sdf.format(process.getDate()) + ")";
        } else if (processDeploymentInfo.getDeploymentDate().before(process.getDate())) {
          deployOperation.detectionStatus = DetectionStatus.NEWVERSION;
          deployOperation.report = "The version is new";
        } else {
          deployOperation.detectionStatus = DetectionStatus.OLDVERSION;
          deployOperation.report = "The version is older, you should ignore this process";
        }
        return deployOperation;
      } catch (ProcessDefinitionNotFoundException pe) {
        // this one is not normal : engine just get back the processDefinitionId
      }
    }
    // is the process exist ? 
    try {
      processDefinitionId = bonitaAccessor.processAPI.getLatestProcessDefinitionId(process.getName());
      ProcessDeploymentInfo processDeploymentInfo = bonitaAccessor.processAPI.getProcessDeploymentInfo(processDefinitionId);
      deployOperation.presentDateArtefact = processDeploymentInfo.getDeploymentDate();
      deployOperation.presentVersionArtefact = processDeploymentInfo.getVersion();

      // event if the same process was deployement AFTER, we considere this artefact as a NEWVERSION
      deployOperation.detectionStatus = DetectionStatus.NEWVERSION;
      deployOperation.report = "The process with this version does not exist, deploy this version";

    } catch (ProcessDefinitionNotFoundException pe) {
      deployOperation.detectionStatus = DetectionStatus.NEWARTEFAC;
      deployOperation.report = "This process is completely new";

    }
    return deployOperation;
  }

  /**
   * 
   */
  public DeployOperation deploy(Artefact process, BonitaAccessor bonitaAccessor, LoggerStore logBox) {
    DeployOperation deployOperation = new DeployOperation();
    deployOperation.deploymentStatus = DeploymentStatus.NOTHINGDONE;

    // artefact is the process
    boolean doLoad = false;

    try {
      Long processDefinitionId = bonitaAccessor.processAPI.getProcessDefinitionId(process.getName(), process.getVersion());
      ProcessDeploymentInfo processDeploymentInfo = bonitaAccessor.processAPI.getProcessDeploymentInfo(processDefinitionId);

      if (processDeploymentInfo.getActivationState() == ActivationState.ENABLED)
        bonitaAccessor.processAPI.disableProcess(processDefinitionId);

      bonitaAccessor.processAPI.deleteProcessDefinition(processDefinitionId);
      doLoad = true;

    } catch (ProcessDefinitionNotFoundException e) {
      doLoad = true; // this not exist
    } catch (DeletionException e) {
      deployOperation.listEvents.add(new BEvent(EventCantRemoveCurrentProcess, e, "Process Name[" + process.getName() + "] Version[" + process.getVersion() + "]"));
      doLoad = false;
    } catch (ProcessActivationException e) {
      deployOperation.listEvents.add(new BEvent(EventCantRemoveCurrentProcess, e, "Process Name[" + process.getName() + "] Version[" + process.getVersion() + "]"));
      doLoad = false;
    }

    if (doLoad) {

      // deploy it
      try {
        // bonitaAccessor.processAPI.deployAndEnableProcess(businessArchive);
        ProcessDefinition processDefinition = bonitaAccessor.processAPI.deploy(((ArtefactProcess) process).getBusinessArchive());
        bonitaAccessor.processAPI.enableProcess(processDefinition.getId());

        deployOperation.deploymentStatus = DeploymentStatus.DEPLOYED;

      } catch (ProcessDeployException e) {
        deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
        deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, e, "Process " + process.getName() + "/" + process.getVersion()));
      } catch (ProcessDefinitionNotFoundException e) {
        deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
        deployOperation.listEvents.add(new BEvent(EventErrorAtEnablement, e, "Process " + process.getName() + "/" + process.getVersion()));
      } catch (ProcessEnablementException e) {
        deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
        deployOperation.listEvents.add(new BEvent(EventErrorAtEnablement, e, "Process " + process.getName() + "/" + process.getVersion()));
      } catch (AlreadyExistsException e) {
        deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
        deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, e, "Process " + process.getName() + "/" + process.getVersion()));
      }
    }
    return deployOperation;
  }
}
