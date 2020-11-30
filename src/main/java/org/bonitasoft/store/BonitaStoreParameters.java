package org.bonitasoft.store;

import java.util.Arrays;
import java.util.List;

import org.bonitasoft.store.artifact.Artifact.TypeArtifact;

/**
 * This class groups all parameters needed at one moment, for the detection of the deployment
 * 
 * @param listTypeApps
 * @param withNotAvailable
 * @param logBox
 * @return
 */
public class BonitaStoreParameters {

    /**
     * List of artifacts to detect or to deploy
     */
    public List<TypeArtifact> listTypeArtifacts = Arrays.asList(TypeArtifact.CUSTOMPAGE, TypeArtifact.CUSTOMWIDGET, TypeArtifact.GROOVY, TypeArtifact.PROCESS, TypeArtifact.BDM, TypeArtifact.LAYOUT, TypeArtifact.LIVINGAPP, TypeArtifact.THEME, TypeArtifact.RESTAPI, TypeArtifact.PROFILE,
            TypeArtifact.ORGANIZATION, TypeArtifact.LOOKANDFEEL);

    public boolean withNotAvailable = true;

    public boolean isByTopics = true;

    public enum POLICY_NEWVERSION {
        BYDATE, NEWVERSION
    };

    public POLICY_NEWVERSION policyNewVersion = POLICY_NEWVERSION.BYDATE;

    /** on a deployment, set the process as ENABLE */
    public boolean processEnable = false;

    /** if true, a actor "ProcessManager" is search. If one if found, then this actor definition is used to setup the process manager */
    public boolean processManagerActor = false;

    /**
     * if true, the process description is analyze for a string "Category:<name>,<name>" and if found, process is registered in theses category
     */
    public boolean processCategory;

}
