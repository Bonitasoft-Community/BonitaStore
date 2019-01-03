package org.bonitasoft.store;

import java.util.List;

import org.bonitasoft.store.ArtefactItem.TypeArtefacts;
import org.bonitasoft.store.BonitaStore.UrlToDownload;
import org.bonitasoft.store.toolbox.LogBox;

public abstract class BonitaStore {

 

  
  
  public abstract String getName();

  /**
   * source is a list of repository (a github repository for example)
   * @param listTypeApps
   * @param withNotAvailable
   * @param logBox
   * @return
   */
  public abstract StoreResult getListContent(final List<TypeArtefacts> listTypeArtefact, boolean withNotAvailable, final LogBox logBox);
  
  /**
   * source is a directory, and it return a list of artefacts 
   * @param listTypeApps
   * @param withNotAvailable
   * @param logBox
   * @return
   */
  public abstract StoreResult getListFiles(final List<TypeArtefacts> listTypeArtefact, boolean withNotAvailable, final LogBox logBox);

  public enum UrlToDownload { LASTRELEASE, URLCONTENT, URLDOWNLOAD } ;

  
  public abstract StoreResult downloadOneArtefact(final ArtefactItem artefactItem, UrlToDownload urlToDownload, boolean isBinaryContent, final LogBox logBox);

}
