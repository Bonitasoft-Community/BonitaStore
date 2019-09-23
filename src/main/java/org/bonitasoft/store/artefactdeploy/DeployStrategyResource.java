package org.bonitasoft.store.artefactdeploy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import org.bonitasoft.engine.page.Page;
import org.bonitasoft.engine.page.PageNotFoundException;
import org.bonitasoft.engine.page.PageSearchDescriptor;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.artefact.Artefact;
import org.bonitasoft.store.artefact.ArtefactAbstractResource;
import org.bonitasoft.store.toolbox.LoggerStore;

public class DeployStrategyResource extends DeployStrategy {

    
    /**
     * detect if the page is present or not
     */
    public DeployOperation detectDeployment(Artefact artefact, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        DeployOperation deployOperation = new DeployOperation();
        ArtefactAbstractResource artefactResource = (ArtefactAbstractResource) artefact;
        try {
            SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 100);
            searchOptionsBuilder.filter(PageSearchDescriptor.CONTENT_TYPE, artefactResource.getContentType());
            final SearchResult<Page> searchResultPage = bonitaAccessor.pageAPI.searchPages(searchOptionsBuilder.done());
            String logToDebug = "DeployStrategyResource.DetectDeployment: Detect for[" + artefactResource.getContentType() + "]";
            for (final Page page : searchResultPage.getResult()) {
                // logToDebug += "[" + page.getName() + "/" + page.getContentName() + "/" + page.getContentType() + "]";
                if (page.getName().equals(artefactResource.getName()) && page.getContentType().equals(artefactResource.getContentType())) {
                    
                    artefact.bonitaBaseElement = page;
                    
                    deployOperation.presentDateArtefact = page.getLastModificationDate();
                    deployOperation.presentVersionArtefact = null;
                    logToDebug += "Found existing page deployed at " + page.getLastModificationDate();
                }
            }
            if (deployOperation.presentDateArtefact == null)
                logToDebug += "Not exist;";
            logBox.info(logToDebug);
        } catch (Exception e) {
            deployOperation.detectionStatus = DetectionStatus.DETECTIONFAILED;
            deployOperation.listEvents.add(new BEvent(EventErrorAtDetection, e, "Page [" + artefactResource.getName() + "]"));
            logBox.severe("DeployStrategyResource: DetectionFailed " + e.getMessage());
        }
        // do not update the deployStatus: the synchronize will do it
        return deployOperation;
    }

    /**
     * Deploy
     */
    public DeployOperation deploy(Artefact artefact, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        DeployOperation deployOperation = new DeployOperation();
        //
        ArtefactAbstractResource artefactResource = (ArtefactAbstractResource) artefact;
        String logToDebug = "DeployStrategyResource.deploy: Starting for[" + artefactResource.getName() + "];";

        Page currentPage = null;
        try {
            currentPage = bonitaAccessor.pageAPI.getPageByName(artefactResource.getName());
            logToDebug += "Replace existing page;";
        } catch (PageNotFoundException pe) {
        }

        try {
            // getPageByName does not work : search manually
            /*
             * final SearchResult<Page> searchResult = pageAPI.searchPages(new
             * SearchOptionsBuilder(0, 1000).done()); for (final Page page :
             * searchResult.getResult()) { if
             * (page.getName().equalsIgnoreCase(foodTruckParam.appsItem.
             * getAppsName())) { pageAPI.deletePage(page.getId()); } }
             */

            /**
             * EXIT
             */
            if (currentPage != null) {
                logToDebug += "Strategy ["+getUpdateStrategy().toString()+"] currentPageId["+currentPage.getId()+"];";
                if (getUpdateStrategy() == UPDATE_STRATEGY.UPDATE)
                {
                    bonitaAccessor.pageAPI.updatePageContent(currentPage.getId(), artefactResource.getContent().toByteArray());
                    artefactResource.bonitaBaseElement = currentPage;
                }
                else
                {
                    logToDebug += "Delete ["+currentPage.getId()+"];";
                    bonitaAccessor.pageAPI.deletePage(currentPage.getId());
                    Page page = bonitaAccessor.pageAPI.createPage(artefactResource.getName(), artefactResource.getContent().toByteArray());
                    artefactResource.bonitaBaseElement = page;
  
                }
                
            } else {
                logToDebug += "Create;";

                Page page = bonitaAccessor.pageAPI.createPage(artefactResource.getName(), artefactResource.getContent().toByteArray());
                artefactResource.bonitaBaseElement = page;

            }
            deployOperation.deploymentStatus = DeploymentStatus.DEPLOYED;
            logToDebug += "Deployed PageId["+artefactResource.bonitaBaseElement.getId()+"];";
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
}
