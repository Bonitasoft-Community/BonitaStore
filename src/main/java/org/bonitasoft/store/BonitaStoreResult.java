package org.bonitasoft.store;

import java.util.ArrayList;
import java.util.List;

import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.toolbox.LoggerStore;

/**
 * Result on any operation on a store
 */
public class BonitaStoreResult {

    // public List<Artefact> listStoreItem = new ArrayList<Artefact>();
    private final List<BEvent> listEvents = new ArrayList<BEvent>();
    public List<Profile> allListProfiles = new ArrayList<Profile>();

    public Long profileEntry = null;

    public String statusTitle;
    public String statusinfo = "";
    public String statusDetails = "";

    public boolean isAllowAddProfile = false;

    public List<Artifact> listArtifacts = new ArrayList<Artifact>();
    // public Map<String, Object> mStatusResultJson = new HashMap<String,
    // Object>();

    /**
     * when a download is required, the content may be a Binary or a String;
     */
    public byte[] contentByte;
    public String content;

    /** time to do the operation in ms */
    private long beginTime;
    private long timeOperation;

    /** create the result as soon as possible to register the time */
    public BonitaStoreResult(final String title) {
        beginOperation();
        statusTitle = title;
    }

    /**
     * if one error message is set, then the status are in error
     *
     * @return
     */
    public boolean isError() {
        return BEventFactory.isError(listEvents);
    }

    public void addDetails(final String details) {
        statusDetails += details + ";";
    }

    public void addEvent(final BEvent event) {
        BEventFactory.addEventUniqueInList(listEvents, event);
        if (event.isError()) {
            LoggerStore.logger.severe("FoodTruck.toolbox: Error " + event.toString());
        }
    }

    public void addEvents(final List<BEvent> events) {
        BEventFactory.addListEventsUniqueInList(listEvents, events);
        for (final BEvent event : events) {
            if (event.isError()) {
                LoggerStore.logger.severe("FoodTruck.toolbox: Error " + event.toString());
            }
        }
    }

    public List<BEvent> getEvents() {
        return listEvents;
    };

    public void setSuccess(final String success) {
        statusinfo = success;

    }

    public void addResult(final BonitaStoreResult statusOperation) {
        statusTitle += statusOperation.statusTitle + ";";
        statusinfo += statusOperation.statusinfo.length() > 0 ? statusOperation.statusinfo + ";" : "";
        statusDetails += statusOperation.statusDetails.length() > 0 ? statusOperation.statusDetails + ";" : "";
        listEvents.addAll(statusOperation.listEvents);
        listArtifacts.addAll(statusOperation.listArtifacts);
    }

    /**
     * get one artefact by its name
     * 
     * @param name
     * @param ignoreCase
     * @return
     */
    public Artifact getArtefactByName(final String name) {
        for (final Artifact apps : listArtifacts) {
            if (apps.getName().equalsIgnoreCase(name)) {
                return apps;
            }
        }
        return null;

    }

    /**
     * timeOperation method
     */
    public void beginOperation() {
        beginTime = System.currentTimeMillis();
    }

    public void endOperation() {
        timeOperation = System.currentTimeMillis() - beginTime;
    }

    public long getTimeOperation() {
        return timeOperation;
    }

}
