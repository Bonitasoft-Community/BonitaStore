package org.bonitasoft.store;

import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.artefact.Artefact;
import org.bonitasoft.store.toolbox.LoggerStore;

/* ******************************************************************************** */
/*                                                                                  */
/* BonitaStoreLocale */
/*                                                                                  */
/* Load the different artefact from the local Bonita Server.                        */
/*                                                                                  */
/* ******************************************************************************** */

public class BonitaStoreLocal extends BonitaStore {
    private static BEvent EVENT_NOT_IMPLEMENTED = new BEvent(BonitaStoreLocal.class.getName(), 1, Level.APPLICATIONERROR, "Not yet implemented", "The function is not yet implemented", "No valid return", "Wait the implementation");
    
    APISession apiSession;
    public BonitaStoreLocal(APISession apiSession)
    {
        this.apiSession= apiSession;
    }
    @Override
    public String getName() {
       return "BonitaStoreLocal";
    }

    @Override
    public Map<String, Object> toMap() {
        return new HashMap<String,Object>();
    }

    @Override
    public StoreResult getListArtefacts(DetectionParameters detectionParameters, LoggerStore logBox) {
        StoreResult storeResult = new StoreResult("getListContent");
        storeResult.addEvent( EVENT_NOT_IMPLEMENTED);
        return storeResult;
    }

    @Override
    public StoreResult downloadArtefact(Artefact artefactItem, UrlToDownload urlToDownload, LoggerStore logBox) {
        StoreResult storeResult = new StoreResult("getListContent");
        storeResult.addEvent( EVENT_NOT_IMPLEMENTED);
        return storeResult;
    }

    @Override
    public StoreResult ping(LoggerStore logBox) {
        return new StoreResult("ping");
    }

}
