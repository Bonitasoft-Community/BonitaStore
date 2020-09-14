package org.bonitasoft.store.artifactdeploy;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.BonitaStoreParameters;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.toolbox.LoggerStore;

public abstract class DeployStrategy {

    protected static BEvent EventErrorAtDeployment = new BEvent(DeployStrategy.class.getName(), 1, Level.APPLICATIONERROR, "Error at deployment", "The Process can't be deployed", "Process is not accessible", "Check the exception");
    protected static BEvent EventErrorAtEnablement = new BEvent(DeployStrategy.class.getName(), 3, Level.APPLICATIONERROR, "Error at Enablement", "The Process is deployment, but not enabled", "Process can't be used", "Check the error in the administration part");
    protected static BEvent EventErrorAtDetection = new BEvent(DeployStrategy.class.getName(), 4, Level.APPLICATIONERROR, "Error at detection", "Detection on the server for this artifact failed, can't know if the artifact exist or not", "artifact can't be deployed", "Check the exception");

    public enum UPDATE_STRATEGY {
        UPDATE, DELETEANDADD
    };

    public UPDATE_STRATEGY updateStrategy = UPDATE_STRATEGY.UPDATE;

    /* ******************************************************************************** */
    /*                                                                                  */
    /* This is the DesignPatern Strategy implementation to deploy an artifact */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    public static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:MM:ss");

    public enum DetectionStatus {
        NEWARTEFAC, SAME, OLDVERSION, NEWVERSION, DETECTIONFAILED, UNDETERMINED
    };

    public enum Action {
        DEPLOY, IGNORE, DELETE
    };

    /**
     * LOADED : artifact is on the server, but not 100% deployed (example, process can't be enabled). This stats does not exist for all artifact (only for Process) 
    *  DEPLOYED : artifact are 100% operational 
     *
     */
    public enum DeploymentStatus {
        NOTHINGDONE, NEWALREADYINPLACE, REMOVEFAIL, LOADFAILED, DEPLOYEDFAILED, LOADED, DEPLOYED, DELETED, BADBONITAVERSION
    };

    public static class DeployOperation {

        public Artifact artifact;
        /**
         * in case of detection, the deployStatus is updated
         */
        public DetectionStatus detectionStatus;
        public Action action;
        /**
         * in case of deployment, the deployStatus is updated
         */
        public DeploymentStatus deploymentStatus;

        public Date presentDateArtifact;
        public String presentVersionArtifact;

        public List<BEvent> listEvents = new ArrayList<>();
        
        public StringBuilder analysis = new StringBuilder();
        public void addAnalysisLine(String line) {
            analysis.append(line+"\n");
            addReportLine( line );
        };
        
        public StringBuilder report = new StringBuilder();
        public void addReportLine(String line) {
            report.append(line+"\n");
        }
    }

    public UPDATE_STRATEGY getUpdateStrategy() {
        return updateStrategy;
    }

    public void setUpdateStrategy(UPDATE_STRATEGY updateStrategy) {
        this.updateStrategy = updateStrategy;
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /*                                                                                  */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    public abstract DeployOperation detectDeployment(Artifact artifact, BonitaStoreParameters deployParameters, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox);

    public abstract DeployOperation deploy(Artifact artifact, BonitaStoreParameters deployParameters, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox);

}
