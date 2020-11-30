package org.bonitasoft.store.artifactdeploy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import org.bonitasoft.engine.page.Page;
import org.bonitasoft.engine.page.PageSearchDescriptor;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.BonitaStoreParameters;
import org.bonitasoft.store.BonitaStoreParameters.POLICY_NEWVERSION;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.ArtifactAbstractResource;
import org.bonitasoft.store.toolbox.LoggerStore;

public class DeployStrategyResource extends DeployStrategy {

    /**
     * detect if the page is present or not
     */
    public DeployOperation detectDeployment(Artifact artifact, BonitaStoreParameters deployParameters, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        DeployOperation deployOperation = new DeployOperation();
        deployOperation.detectionStatus = DetectionStatus.NEWARTEFAC;

        ArtifactAbstractResource artifactResource = (ArtifactAbstractResource) artifact;
        try {
            deployOperation.addReportLine("DeployStrategyResource.DetectDeployment: Detect for[" + artifactResource.getBonitaName() + "] Type[" + artifactResource.getContentType() + "];");
            Page page = searchPage(artifactResource, bonitaAccessor);
            if (page != null) {
                artifactResource.bonitaBaseElement = page;
                deployOperation.presentDateArtifact = page.getLastModificationDate();
                deployOperation.presentVersionArtifact = null;
                deployOperation.addReportLine("Found existing page deployed at " + DeployStrategy.sdf.format(page.getLastModificationDate()) + ";");
                deployOperation.addReportLine("POLICY=" + artifact.getPolicyNewVersion(deployParameters.policyNewVersion).toString() + ";");

                if (POLICY_NEWVERSION.BYDATE.equals(artifact.getPolicyNewVersion(deployParameters.policyNewVersion))) {
                    Date lastDateArtifact = artifactResource.getLastDateArtifact();
                    deployOperation.addReportLine("Local resource Date: " + (lastDateArtifact == null ? "null" : DeployStrategy.sdf.format(lastDateArtifact)) + ";");
                    if (lastDateArtifact != null && lastDateArtifact.before(page.getLastModificationDate())) {
                        deployOperation.addReportLine("BEFORE, consider as SAME");
                        deployOperation.detectionStatus = DetectionStatus.SAME; // or OLD...
                        deployOperation.addAnalysisLine("A version exists with the date more recent(" + DeployStrategy.sdf.format(page.getLastModificationDate()) + ")");
                    } else {
                        deployOperation.addReportLine("AFTER, consider as NEW");
                        deployOperation.detectionStatus = DetectionStatus.NEWVERSION;
                        deployOperation.addAnalysisLine("The version is new");
                    }
                } else {
                    // well, no way to know
                    deployOperation.addReportLine("No policy");
                    deployOperation.detectionStatus = DetectionStatus.UNDETERMINED;
                }
            }
            if (deployOperation.presentDateArtifact == null)
                deployOperation.addReportLine("Not exist;");
            logBox.info(deployOperation.analysis.toString());
        } catch (Exception e) {
            deployOperation.detectionStatus = DetectionStatus.DETECTIONFAILED;
            deployOperation.listEvents.add(new BEvent(EventErrorAtDetection, e, "Page [" + artifactResource.getName() + "]"));
            deployOperation.addReportLine("Exception " + e.getMessage());
            logBox.severe("DeployStrategyResource: DetectionFailed " + e.getMessage());
        }
        // do not update the deployStatus: the synchronize will do it
        return deployOperation;
    }

    /**
     * Deploy
     */
    public DeployOperation deploy(Artifact artefact, BonitaStoreParameters deployParameters, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        DeployOperation deployOperation = new DeployOperation();
        //
        ArtifactAbstractResource artefactResource = (ArtifactAbstractResource) artefact;
        String logToDebug = "DeployStrategyResource.deploy: Starting for[" + artefactResource.getName() + "];";

        Page page = searchPage(artefactResource, bonitaAccessor);

        try {
            /**
             * EXIT
             */
            if (page != null) {
                logToDebug += "Strategy [" + getUpdateStrategy().toString() + "] currentPageId[" + page.getId() + "];";
                if (getUpdateStrategy() == UPDATE_STRATEGY.UPDATE) {
                    bonitaAccessor.pageAPI.updatePageContent(page.getId(), artefactResource.getContent().toByteArray());
                    artefactResource.bonitaBaseElement = page;
                } else {
                    logToDebug += "Delete [" + page.getId() + "];";
                    bonitaAccessor.pageAPI.deletePage(page.getId());
                    page = bonitaAccessor.pageAPI.createPage(artefactResource.getName(), artefactResource.getContent().toByteArray());
                    artefactResource.bonitaBaseElement = page;

                }

            } else {
                logToDebug += "Create;";

                page = bonitaAccessor.pageAPI.createPage(artefactResource.getName(), artefactResource.getContent().toByteArray());
                artefactResource.bonitaBaseElement = page;

            }
            deployOperation.deploymentStatus = DeploymentStatus.DEPLOYED;
            logToDebug += "Deployed PageId[" + artefactResource.bonitaBaseElement.getId() + "];";
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
            deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, e, "Page [" + artefactResource.getName() + "]"));
            logToDebug += "Error at deployment [" + e.getMessage() + " : " + exceptionDetails + "]";
        }
        logBox.info(logToDebug);

        return deployOperation;
    }

    private Page searchPage(ArtifactAbstractResource artefactResource, BonitaStoreAccessor bonitaAccessor) {
        try {

            // pageAPI.getPageByName() doesn't work, search manually
            SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 10000);
            searchOptionsBuilder.filter(PageSearchDescriptor.CONTENT_TYPE, artefactResource.getContentType());
            final SearchResult<Page> searchResultPage = bonitaAccessor.pageAPI.searchPages(searchOptionsBuilder.done());
            // String logToDebug = "DeployStrategyResource.DetectDeployment: Detect for[" + artefactResource.getContentType() + "]";
            for (final Page page : searchResultPage.getResult()) {
                // logToDebug += "[" + page.getName() + "/" + page.getContentName() + "/" + page.getContentType() + "]";
                if (page.getName().equalsIgnoreCase(artefactResource.getName()) && page.getContentType().equals(artefactResource.getContentType())) {
                    return page;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
