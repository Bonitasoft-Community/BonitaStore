package org.bonitasoft.store.artefactdeploy;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaAccessor;
import org.bonitasoft.store.artefact.Artefact;
import org.bonitasoft.store.toolbox.LoggerStore;

public abstract class DeployStrategy {

  protected static BEvent EventErrorAtDeployment = new BEvent(DeployStrategy.class.getName(), 1, Level.APPLICATIONERROR, "Error at deployment", "The Process can't be deployed", "Process is not accessible", "Check the exception");
  protected static BEvent EventErrorAtEnablement = new BEvent(DeployStrategy.class.getName(), 3, Level.APPLICATIONERROR, "Error at Enablement", "The Process is deployment, but not enable", "Process can't be used", "Check the error in the administration part");
  protected static BEvent EventErrorAtDetection = new BEvent(DeployStrategy.class.getName(), 4, Level.APPLICATIONERROR, "Error at detection", "Detection on the server for this artefact failed, can't know if the artefact exist or not", "Artefact can't be deployed", "Check the exception");

  /* ******************************************************************************** */
  /*                                                                                  */
  /* This is the DesignPatern Strategy implementation to deploy an artefact */
  /*                                                                                  */
  /*                                                                                  */
  /* ******************************************************************************** */
  public static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:MM:ss");

  public enum DetectionStatus {
    NEWARTEFAC, SAME, OLDVERSION, NEWVERSION, DETECTIONFAILED
  };

  public enum Action {
    DEPLOY, IGNORE, DELETE
  };

  public enum DeploymentStatus {
    NOTHINGDONE, NEWALREADYINPLACE, REMOVEFAIL, LOADFAILED, DEPLOYEDFAILED, DEPLOYED, DELETED, BADBONITAVERSION
  };

  public static class DeployOperation {

    public Artefact artefact;
    /**
     * in case of detection, the deployStatus is updated
     */
    public DetectionStatus detectionStatus;
    public Action action;
    /**
     * in case of deployment, the deployStatus is updated
     */
    public DeploymentStatus deploymentStatus;

    public Date presentDateArtefact;
    public String presentVersionArtefact;

    public List<BEvent> listEvents = new ArrayList<BEvent>();
    public String report;
  }

  /* ******************************************************************************** */
  /*                                                                                  */
  /*                                                                                  */
  /*                                                                                  */
  /*                                                                                  */
  /* ******************************************************************************** */
  public abstract DeployOperation detectDeployment(Artefact artefact, BonitaAccessor bonitaAccessor, LoggerStore logBox);

  public abstract DeployOperation deploy(Artefact artefact, BonitaAccessor bonitaAccessor, LoggerStore logBox);

}
