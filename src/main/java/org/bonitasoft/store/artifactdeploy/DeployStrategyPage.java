package org.bonitasoft.store.artifactdeploy;

public class DeployStrategyPage extends DeployStrategyResource {

    /*
     * keep the DeployStrategyResource one
     * @Override
     * public DeployOperation detectDeployment(Artifact artifact, BonitaStoreParameters deployParameters, BonitaStoreAccessor bonitaAccessor, LoggerStore
     * logBox) {
     * DeployOperation deployOperation = new DeployOperation();
     * deployOperation.detectionStatus = DetectionStatus.NEWVERSION;
     * Page page = null;
     * try {
     * page = bonitaAccessor.pageAPI.getPageByName(artifact.getName());
     * deployOperation.presentDateArtifact = page.getLastModificationDate();
     * artifact.bonitaBaseElement = page;
     * if (POLICY_NEWVERSION.BYDATE.equals(artifact.getPolicyNewVersion(deployParameters.policyNewVersion))) {
     * if (artifact.getLastReleaseDate().before(page.getLastModificationDate())) {
     * deployOperation.detectionStatus = DetectionStatus.SAME; // or OLD...
     * deployOperation.report = "A version exists with the date more recent(" + DeployStrategy.sdf.format(page.getLastModificationDate()) + ")";
     * } else {
     * deployOperation.detectionStatus = DetectionStatus.NEWVERSION;
     * deployOperation.report = "The version is new";
     * }
     * } else {
     * // well, no way to know
     * deployOperation.detectionStatus = DetectionStatus.UNDETERMINED;
     * }
     * } catch (PageNotFoundException e) {
     * deployOperation.detectionStatus = DetectionStatus.NEWARTEFAC;
     * deployOperation.report = "This process is completely new";
     * }
     * return deployOperation;
     * }
     */

    /**
     * jkeep the deploystrategy one
     * 
     * @Override
     *           public DeployOperation deploy(Artifact artefact, BonitaStoreParameters deployParameters, BonitaStoreAccessor bonitaAccessor, LoggerStore
     *           logBox) {
     *           DeployOperation deployOperation = new DeployOperation();
     *           //
     *           Page page = null;
     *           try {
     *           page = bonitaAccessor.pageAPI.getPageByName(artefact.getName());
     *           // getPageByName does not work : search manually
     *           } catch (PageNotFoundException e) {
     *           }
     *           try {
     *           if (page != null) {
     *           bonitaAccessor.pageAPI.updatePageContent(page.getId(), artefact.getContent().toByteArray());
     *           } else {
     *           bonitaAccessor.pageAPI.createPage(artefact.getName(), artefact.getContent().toByteArray());
     *           }
     *           deployOperation.deploymentStatus = DeploymentStatus.DEPLOYED;
     *           } catch (Exception e) {
     *           deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
     *           deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, e, "Page [" + artefact.getName() + "]"));
     *           }
     *           return deployOperation;
     *           }
     */

}
