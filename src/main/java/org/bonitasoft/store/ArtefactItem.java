package org.bonitasoft.store;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.page.Page;
import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.log.event.BEvent;
import org.json.simple.JSONValue;



/**
 * an AppsItem maybe a Custom Page, a CustomWidget, ...
 */
public class ArtefactItem {

	public class ArtefactRelease {

		public Long id;
		public String version;
		public Date dateRelease;
		public String urlDownload;
		public Long numberOfDownload;
		public String releaseNote;
	}

	public enum TypeArtefacts {
		CUSTOMPAGE, CUSTOMWIDGET, GROOVY, ELSE
	};

	/**
	 * name of the application. THis name is unique on the Store and locally
	 */
	private String artefactName;


	public TypeArtefacts typeArtefact;

	public String displayName;
	public String contribFile;

	public String documentationFile;
	public String description;
	
	public String urlContent;
	
	public String urlDownload;
	
	public byte[] logo;

	/*
	 * according the source, the artefact may be write as "non available"
	 * git : last release does not have a link to download
	 * bonita server: artefact may be not enable
	 */
	public boolean isAvailable = true;
	/**
	 * multiple github source can be explode. Retains from which github this
	 * apps come from
	 */
	public BonitaStore sourceBonitaStore;

	/**
	 * this properties are private : if some release are know, then the
	 * information come the release list. Use the get() method
	 */
	private String lastUrlDownload;
	private Date lastReleaseDate;
	private long numberOfDownload = 0;

	// in case of a new release exist on the store, this is the new release date
	// public Date storeReleaseDate;

	// isProvided : this page is provided by defaut on the BonitaEngine (like
	// the GroovyExample page)
	public boolean isProvided = false;

	/**
	 * calculate the whatsnews between the current version and the store one
	 */
	public String whatsnews;
	// add description... profile...

	public List<BEvent> listEvents = new ArrayList<BEvent>();

	private List<Map<String, Object>> listProfiles = new ArrayList<Map<String, Object>>();

	public final List<ArtefactRelease> listReleases = new ArrayList<ArtefactRelease>();

	public ArtefactItem() {
	};

	
  /**
   * name is in lower case everytime
   */
  public void setArtefactName(final TypeArtefacts typeApps, final String appsName) {

    this.artefactName = appsName == null ? "" : appsName.toLowerCase();
    if (typeApps != null) {
      // normalise the name
      if (this.artefactName.indexOf("_") != -1) {
        this.artefactName = typeApps.toString().toLowerCase() + this.artefactName.substring(this.artefactName.indexOf("_"));
      }
    }
  }
  /**
   * name is in lower case everytime
   * @return
   */
	public String getArtefactName() {
		return artefactName;
	}

	
	@Override
	public String toString() {
		return artefactName + " IsProvided:" + isProvided + " " + description + " URL[" + getLastUrlDownload() + "] nbRelease[" + listReleases.size() + "]";
	}

	// release
	public ArtefactRelease newInstanceRelease() {
		final ArtefactRelease appsRelease = new ArtefactRelease();
		return appsRelease;
	}

	/**
	 * we parse the release. For all realease AFTER the date, we complete the
	 * release note.
	 *
	 * @param dateFrom
	 * @return
	 */
	public String getReleaseInformation(final Date dateFrom) {
		return "";
	}

	public Date getLastReleaseDate() {
		if (listReleases.size() > 0) {
			return listReleases.get(0).dateRelease;
		}
		return lastReleaseDate;
	}

	/**
	 * getNbDownload, by summury all the download in release, or by using the
	 * local information
	 */
	public long getNumberOfDownload() {
		if (listReleases.size() > 0) {
			long total = 0;
			for (final ArtefactRelease appsRelease : listReleases) {
				total += appsRelease.numberOfDownload;
			}
			return total;
		}
		return numberOfDownload;
	}

	public String getLastUrlDownload() {
	  if (listReleases.size() > 0) {
			return listReleases.get(0).urlDownload;
		}
		return lastUrlDownload;
	}

	public void addEvent(final BEvent eventMsg) {
		listEvents.add(eventMsg);
	}
}