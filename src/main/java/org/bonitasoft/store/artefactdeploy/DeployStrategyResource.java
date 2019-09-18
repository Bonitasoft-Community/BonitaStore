package org.bonitasoft.store.artefactdeploy;

import java.util.logging.Logger;

import org.bonitasoft.engine.page.Page;
import org.bonitasoft.engine.page.PageNotFoundException;
import org.bonitasoft.engine.page.PageSearchDescriptor;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.store.BonitaAccessor;
import org.bonitasoft.store.artefact.Artefact;
import org.bonitasoft.store.artefact.ArtefactAbstractResource;
import org.bonitasoft.store.toolbox.LoggerStore;

public class DeployStrategyResource extends DeployStrategy {

  public DeployOperation detectDeployment(Artefact artefact, BonitaAccessor bonitaAccessor, LoggerStore logBox) {
    DeployOperation deployOperation = new DeployOperation();
    ArtefactAbstractResource artefactResource = (ArtefactAbstractResource) artefact;
    try {
      SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 100);
      searchOptionsBuilder.filter(PageSearchDescriptor.CONTENT_TYPE, artefactResource.getContentType());
      final SearchResult<Page> searchResultPage = bonitaAccessor.pageAPI.searchPages(searchOptionsBuilder.done());
      String logToDebug = "";
      for (final Page page : searchResultPage.getResult()) {
        logToDebug += "[" + page.getName() + "/" + page.getContentName() + "/" + page.getContentType() + "]";
        if (page.getName().equals(artefactResource.getName()) && page.getContentType().equals(artefactResource.getContentType())) {
          deployOperation.presentDateArtefact = page.getLastModificationDate();
          deployOperation.presentVersionArtefact = null;
        }
      }
      logBox.info("Detected for[" + artefactResource.getContentType() + "]: " + logToDebug);
    } catch (Exception e) {
      deployOperation.detectionStatus = DetectionStatus.DETECTIONFAILED;
      deployOperation.listEvents.add(new BEvent(EventErrorAtDetection, e, "Page [" + artefactResource.getName() + "]"));

    }
    // do not update the deployStatus: the synchronize will do it
    return deployOperation;
  }

  public DeployOperation deploy(Artefact artefact, BonitaAccessor bonitaAccessor, LoggerStore logBox) {
    DeployOperation deployOperation = new DeployOperation();
    //
    ArtefactAbstractResource artefactResource = (ArtefactAbstractResource) artefact;

    Page currentPage = null;
    try {
      currentPage = bonitaAccessor.pageAPI.getPageByName(artefactResource.getName());
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
        bonitaAccessor.pageAPI.updatePageContent(currentPage.getId(), artefactResource.getContent().toByteArray());
      } else {
        Page page = bonitaAccessor.pageAPI.createPage(artefactResource.getName(), artefactResource.getContent().toByteArray());
      }
      deployOperation.deploymentStatus = DeploymentStatus.DEPLOYED;
    } catch (Exception e) {
      deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
      deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, e, "Page [" + artefactResource.getName() + "]"));
    }
    return deployOperation;
  }
}
