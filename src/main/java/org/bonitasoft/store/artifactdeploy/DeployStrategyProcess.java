package org.bonitasoft.store.artifactdeploy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.bonitasoft.engine.bpm.actor.ActorCriterion;
import org.bonitasoft.engine.bpm.actor.ActorInstance;
import org.bonitasoft.engine.bpm.actor.ActorMember;
import org.bonitasoft.engine.bpm.category.Category;
import org.bonitasoft.engine.bpm.category.CategoryCriterion;
import org.bonitasoft.engine.bpm.process.ActivationState;
import org.bonitasoft.engine.bpm.process.ProcessActivationException;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessDeployException;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ProcessEnablementException;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.BonitaStoreParameters;
import org.bonitasoft.store.BonitaStoreParameters.POLICY_NEWVERSION;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.ArtifactProcess;
import org.bonitasoft.store.toolbox.LoggerStore;

public class DeployStrategyProcess extends DeployStrategy {

    private static final String CST_CATEGORY = "CATEGORY:";

    private static final String CST_PROCESS_MANAGER = "ProcessManager";

    protected final static BEvent EventCantRemoveCurrentProcess = new BEvent(DeployStrategyProcess.class.getName(), 1, Level.APPLICATIONERROR, "Current process can't be removed", "To deploy the new process (same name, same version), current process has to be removed. The operation failed.",
            "Deployment of the new process is not possible", "Check the exception");

    protected final static BEvent EventProcessNotLoaded = new BEvent(DeployStrategyProcess.class.getName(), 2, Level.APPLICATIONERROR, "Process is not loaded", "Proces has to be loaded first.",
            "Deployment of the new process is not possible", "Check the error");

    protected final static BEvent EventProcessManagerCreated = new BEvent(DeployStrategyProcess.class.getName(), 3, Level.SUCCESS, "Process Manager created", "A Process Manager is created from the actor " + CST_PROCESS_MANAGER);

    protected final static BEvent EventProcessManagerCreationError = new BEvent(DeployStrategyProcess.class.getName(), 4, Level.SUCCESS, "Process Manager created", "A Process Manager is created from the actor " + CST_PROCESS_MANAGER);

    protected final static BEvent EventProcessCategoryError = new BEvent(DeployStrategyProcess.class.getName(), 5, Level.APPLICATIONERROR, "Can't create a category", "A category can't be created", "Process is not registered in the category", "Check the exception");

    
    protected final static BEvent EventProcessCategoryReferenced = new BEvent(DeployStrategyProcess.class.getName(), 6, Level.SUCCESS, "Process Manager referenced in category", "Process is referenced in category");

    /* *********************************************************************** */
    /*                                                                         */
    /* Deployment */
    /*                                                                         */
    /*                                                                         */
    /* *********************************************************************** */

    @Override
    public DeployOperation detectDeployment(Artifact process, BonitaStoreParameters deployParameters, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        DeployOperation deployOperation = new DeployOperation();
        deployOperation.detectionStatus = DetectionStatus.NEWVERSION;

        Long processDefinitionId = null;
        try {
            // bob
            SearchOptionsBuilder sob = new SearchOptionsBuilder(0, 100);
            sob.filter(ProcessDeploymentInfoSearchDescriptor.NAME, process.getName());
            SearchResult<ProcessDeploymentInfo> searchResult = bonitaAccessor.processAPI.searchProcessDeploymentInfos(sob.done());
            // search for the same version maybe
            for (ProcessDeploymentInfo processDeploymentInfo : searchResult.getResult())
            {
                // all the process should have the same name, so set it because the file may not respect the correct casse
                if (processDefinitionId==null) {
                    processDefinitionId = processDeploymentInfo.getProcessId();
                    process.setName(processDeploymentInfo.getName());
                    process.setDisplayName(processDeploymentInfo.getDisplayName());
                }

                // if we have the same version here, then set the processDefinition
                if (processDeploymentInfo.getVersion().equalsIgnoreCase(process.getVersion())) {
                    processDefinitionId= processDeploymentInfo.getProcessId();
                    process.setVersion( processDeploymentInfo.getVersion() );
                }
            }
            // processDefinitionId = bonitaAccessor.processAPI.getProcessDefinitionId(process.getName(), process.getVersion());

        } catch ( SearchException pe) {
            // nothing to do here
        }

        if (processDefinitionId==null)
        {
            deployOperation.detectionStatus = DetectionStatus.NEWARTEFAC;
            deployOperation.report = "This process is completely new";
        } else {
            try {
                ProcessDeploymentInfo processDeploymentInfo = bonitaAccessor.processAPI.getProcessDeploymentInfo(processDefinitionId);
                deployOperation.presentDateArtifact = processDeploymentInfo.getDeploymentDate();
                deployOperation.presentVersionArtifact = process.getVersion();
                // we base the deployment status on what ? 
                if (POLICY_NEWVERSION.BYDATE.equals( process.getPolicyNewVersion( deployParameters.policyNewVersion))) 
                {
                    if (processDeploymentInfo.getDeploymentDate().equals(process.getDate())) {
    
                        process.bonitaBaseElement = bonitaAccessor.processAPI.getProcessDefinition(processDeploymentInfo.getProcessId());
    
                        deployOperation.detectionStatus = DetectionStatus.SAME;
                        deployOperation.report = "A version exists with the same date (" + DeployStrategy.sdf.format(process.getDate()) + ")";
                    } else if (processDeploymentInfo.getDeploymentDate().before(process.getDate())) {
                        deployOperation.detectionStatus = DetectionStatus.NEWVERSION;
                        deployOperation.report = "The version is new";
                    } else {
                        deployOperation.detectionStatus = DetectionStatus.OLDVERSION;
                        deployOperation.report = "The version is older, you should ignore this process";
                    }
                }
                else {
                    // the process, at this version, must not exist
                    deployOperation.detectionStatus = DetectionStatus.SAME;
                }
                return deployOperation;
            } catch (ProcessDefinitionNotFoundException pe) {
                // this one is not normal : engine just get back the processDefinitionId
            }
        }
        return deployOperation;
    }

    /**
     * 
     */
    @Override
    public DeployOperation deploy(Artifact process, BonitaStoreParameters deployParameters, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        DeployOperation deployOperation = new DeployOperation();
        deployOperation.deploymentStatus = DeploymentStatus.NOTHINGDONE;

        // artifact is the process
        boolean doLoad = true;
        // if the artifact is not loaded, stop now
        if (!process.isLoaded()) {
            deployOperation.listEvents.add(new BEvent(EventProcessNotLoaded, getLabelProcess( process )));
            doLoad = false;
        }

        // first, delete the current one
        if (doLoad) {
            try {
                SearchOptionsBuilder sob = new SearchOptionsBuilder(0, 100);
                sob.filter(ProcessDeploymentInfoSearchDescriptor.NAME, process.getName());
                sob.filter(ProcessDeploymentInfoSearchDescriptor.VERSION, process.getVersion());
                SearchResult<ProcessDeploymentInfo> searchResult = bonitaAccessor.processAPI.searchProcessDeploymentInfos(sob.done());
                if (searchResult.getCount() > 0) {
                    // Case sentitive 
                    // Long processDefinitionId = bonitaAccessor.processAPI.getProcessDefinitionId(process.getName(), process.getVersion());
                    Long processDefinitionId = searchResult.getResult().get(0).getProcessId();
                    ProcessDeploymentInfo processDeploymentInfo = bonitaAccessor.processAPI.getProcessDeploymentInfo(processDefinitionId);

                    if (processDeploymentInfo.getActivationState() == ActivationState.ENABLED)
                        bonitaAccessor.processAPI.disableProcess(processDefinitionId);

                    bonitaAccessor.processAPI.deleteProcessDefinition(processDefinitionId);
                    doLoad = true;
                }
            } catch (ProcessDefinitionNotFoundException | SearchException e) {
                doLoad = true; // this not exist
            } catch (DeletionException | ProcessActivationException e) {
                if (e.getMessage().contains("active process instance"))
                    deployOperation.listEvents.add(new BEvent(EventCantRemoveCurrentProcess, "Active instances in "+getLabelProcess( process ) ));
                else
                    deployOperation.listEvents.add(new BEvent(EventCantRemoveCurrentProcess, e, getLabelProcess( process ) ));
                doLoad = false;
            }
        }
        if (doLoad) {

            try {
                // bonitaAccessor.processAPI.deployAndEnableProcess(businessArchive);
                
                
                ProcessDefinition processDefinition = bonitaAccessor.processAPI.deploy(((ArtifactProcess) process).getBusinessArchive());
                deployOperation.deploymentStatus = DeploymentStatus.LOADED;

                if (deployParameters.processManagerActor)
                    deployOperation.listEvents.addAll(applyProcessManager(processDefinition, bonitaAccessor, logBox));

                if (deployParameters.processCategory)
                    deployOperation.listEvents.addAll(applyCategory(processDefinition, bonitaAccessor, logBox));

                if (deployParameters.processEnable)
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

    /**
     * @param processDefinition
     * @param bonitaAccessor
     * @param logBox
     * @return
     */
    private List<BEvent> applyProcessManager(ProcessDefinition processDefinition, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        List<BEvent> listEvents = new ArrayList<>();

        List<ActorInstance> listActors = bonitaAccessor.processAPI.getActors(processDefinition.getId(), 0, 10000, ActorCriterion.NAME_ASC);
        for (ActorInstance actor : listActors) {
            if (actor.getName().equalsIgnoreCase(CST_PROCESS_MANAGER)) {
                // Here the actor, copy the definition 
                List<ActorMember> listActorMember = bonitaAccessor.processAPI.getActorMembers(actor.getId(), 0, 10000);
                for (ActorMember actorMember : listActorMember) {
                    String explanation = "";
                    try {

                        if (actorMember.getGroupId() > 0 && actorMember.getRoleId() > 0) {
                            explanation = "MemberShip Group[" + actorMember.getGroupId() + "] RoleId[" + actorMember.getRoleId() + "]";
                            bonitaAccessor.processAPI.createProcessSupervisorForMembership(processDefinition.getId(), actorMember.getGroupId(), actorMember.getRoleId());
                        } else if (actorMember.getGroupId() > 0) {
                            explanation = "MemberShip Group[" + actorMember.getGroupId() + "]";
                            bonitaAccessor.processAPI.createProcessSupervisorForGroup(processDefinition.getId(), actorMember.getGroupId());
                        } else if (actorMember.getRoleId() > 0) {
                            explanation = "MemberShip Role[" + actorMember.getRoleId() + "]";
                            bonitaAccessor.processAPI.createProcessSupervisorForRole(processDefinition.getId(), actorMember.getRoleId());
                        } else if (actorMember.getUserId() > 0) {
                            explanation = "MemberShip User[" + actorMember.getUserId() + "]";
                            bonitaAccessor.processAPI.createProcessSupervisorForUser(processDefinition.getId(), actorMember.getUserId());
                        }
                    } catch (AlreadyExistsException e) {
                        // nothing to do here
                    } catch (CreationException e) {
                        listEvents.add(new BEvent(EventProcessManagerCreationError, explanation));
                    }

                } // end actor Member
                listEvents.add(EventProcessManagerCreated);
            }
        }

        return listEvents;
    }

    /**
     * @param processDefinition
     * @param bonitaAccessor
     * @param logBox
     * @return
     */
    private List<BEvent> applyCategory(ProcessDefinition processDefinition, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        List<BEvent> listEvents = new ArrayList<>();
        String description = processDefinition.getDescription();
        if (description == null)
            return listEvents;
        int pos = description.toUpperCase().indexOf(CST_CATEGORY);
        if (pos == -1)
            return listEvents;
        String listCategoryString = description.substring(pos + CST_CATEGORY.length());
        pos = listCategoryString.toUpperCase().indexOf('\n');
        if (pos != -1)
            listCategoryString = listCategoryString.substring(0,pos);
        // search the end of the line
        StringTokenizer st = new StringTokenizer(listCategoryString, ",");
        List<Category> listPortalCategory = bonitaAccessor.processAPI.getCategories(0, 10000, CategoryCriterion.NAME_ASC);
        Map<String,Category> mapPortalCategory = new HashMap<>();
        for (Category categoryI : listPortalCategory) {
            mapPortalCategory.put( categoryI.getName(),  categoryI);
        }
        List<Long> categoryIds = new ArrayList<>();
        // generate one category per string
        String categoryName = "";
        StringBuilder categoryNameList= new StringBuilder();
        try {
            while (st.hasMoreTokens()) {
                categoryName = st.nextToken();
                Category category = mapPortalCategory.get(categoryName);
                if (category == null) {
                    // create it
                    category = bonitaAccessor.processAPI.createCategory(categoryName, "");
                    // in case user gives twice the name in the list
                    mapPortalCategory.put(category.getName(), category);
                }
                categoryIds.add(category.getId());
                categoryNameList.append(category.getName()+",");
            }
            if (categoryIds.isEmpty())
                return listEvents;

            bonitaAccessor.processAPI.addCategoriesToProcess(processDefinition.getId(), categoryIds);
            listEvents.add( new BEvent(EventProcessCategoryReferenced, categoryNameList.toString()));

        } catch (CreationException e) {
            listEvents.add(new BEvent(EventProcessCategoryError, e, "Category [" + categoryName + "]"));
        }

        return listEvents;
    }
    
    private String getLabelProcess(Artifact process) {
        return "Process Name[" + process.getName() + "] Version[" + process.getVersion() + "]";
    }
}