package org.bonitasoft.store.artifactdeploy;

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
import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.ArtifactProcess;
import org.bonitasoft.store.toolbox.LoggerStore;

public class DeployStrategyProcess extends DeployStrategy {

    protected final static BEvent EventCantRemoveCurrentProcess = new BEvent(DeployStrategyProcess.class.getName(), 1, Level.APPLICATIONERROR, "Current process can't be removed", "To deploy the new process (same name, same version), current process has to be removed. The operation failed.",
            "Deployment of the new process is not possible", "Check the exception");

    protected final static BEvent EventProcessNotLoaded = new BEvent(DeployStrategyProcess.class.getName(), 2, Level.APPLICATIONERROR, "Process is not loaded", "Proces has to be loaded first.",
            "Deployment of the new process is not possible", "Check the error");

    /* *********************************************************************** */
    /*                                                                         */
    /* Deployement */
    /*                                                                         */
    /*                                                                         */
    /* *********************************************************************** */

    public DeployOperation detectDeployment(Artifact process, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        DeployOperation deployOperation = new DeployOperation();
        Long processDefinitionId = null;
        try {
            processDefinitionId = bonitaAccessor.processAPI.getProcessDefinitionId(process.getName(), process.getVersion());

        } catch (ProcessDefinitionNotFoundException pe) {
            // nothing to do here
        }

        if (processDefinitionId != null) {
            try {
                ProcessDeploymentInfo processDeploymentInfo = bonitaAccessor.processAPI.getProcessDeploymentInfo(processDefinitionId);
                deployOperation.presentDateArtifact = processDeploymentInfo.getDeploymentDate();
                deployOperation.presentVersionArtifact = process.getVersion();
                if (processDeploymentInfo.getDeploymentDate().equals(process.getDate())) {

                    process.bonitaBaseElement = bonitaAccessor.processAPI.getProcessDefinition(processDeploymentInfo.getProcessId());

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
            deployOperation.presentDateArtifact = processDeploymentInfo.getDeploymentDate();
            deployOperation.presentVersionArtifact = processDeploymentInfo.getVersion();

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
    public DeployOperation deploy(Artifact process, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        DeployOperation deployOperation = new DeployOperation();
        deployOperation.deploymentStatus = DeploymentStatus.NOTHINGDONE;

        // artifact is the process
        boolean doLoad = true;
        // if the artifact is not loaded, stop now
        if (!process.isLoaded()) {
            deployOperation.listEvents.add(new BEvent(EventProcessNotLoaded, "Process Name[" + process.getName() + "] Version[" + process.getVersion() + "]"));
            doLoad = false;
        }

        // first, delete the current one
        if (doLoad) {
            try {
                Long processDefinitionId = bonitaAccessor.processAPI.getProcessDefinitionId(process.getName(), process.getVersion());
                ProcessDeploymentInfo processDeploymentInfo = bonitaAccessor.processAPI.getProcessDeploymentInfo(processDefinitionId);

                if (processDeploymentInfo.getActivationState() == ActivationState.ENABLED)
                    bonitaAccessor.processAPI.disableProcess(processDefinitionId);

                bonitaAccessor.processAPI.deleteProcessDefinition(processDefinitionId);
                doLoad = true;

            } catch (ProcessDefinitionNotFoundException e) {
                doLoad = true; // this not exist
            } catch (DeletionException | ProcessActivationException e) {
                deployOperation.listEvents.add(new BEvent(EventCantRemoveCurrentProcess, e, "Process Name[" + process.getName() + "] Version[" + process.getVersion() + "]"));
                doLoad = false;
            }
        }
        if (doLoad) {

            try {
                // bonitaAccessor.processAPI.deployAndEnableProcess(businessArchive);
                ProcessDefinition processDefinition = bonitaAccessor.processAPI.deploy(((ArtifactProcess) process).getBusinessArchive());
                deployOperation.deploymentStatus = DeploymentStatus.LOADED;

                bonitaAccessor.processAPI.enableProcess(processDefinition.getId());

                process.bonitaBaseElement = processDefinition;
                deployOperation.deploymentStatus = DeploymentStatus.DEPLOYED;

            } catch (ProcessDeployException e) {
                deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
                deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, getLoginformation(process)));
            } catch (ProcessDefinitionNotFoundException e) {
                deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
                deployOperation.listEvents.add(new BEvent(EventErrorAtEnablement, e, getLoginformation(process)));
            } catch (ProcessEnablementException e) {
                deployOperation.deploymentStatus = DeploymentStatus.LOADED;
                deployOperation.listEvents.add(new BEvent(EventErrorAtEnablement, getLoginformation(process)));
            } catch (AlreadyExistsException e) {
                deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
                deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, "Already exist " + getLoginformation(process)));
            }
        }
        return deployOperation;
    }

    private String getLoginformation(Artifact process) {
        return "ProcessName[" + process.getName() + "] Version[" + process.getVersion() + "]";
    }

}
