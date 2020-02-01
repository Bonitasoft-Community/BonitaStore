package org.bonitasoft.store.artifact;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStore;
import org.bonitasoft.store.artefactdeploy.DeployStrategyBDM;
import org.bonitasoft.store.artefactdeploy.DeployStrategyLivingApplication;
import org.bonitasoft.store.artefactdeploy.DeployStrategyLookAndFeel;
import org.bonitasoft.store.artefactdeploy.DeployStrategyOrganization;
import org.bonitasoft.store.artefactdeploy.DeployStrategyProcess;
import org.bonitasoft.store.artefactdeploy.DeployStrategyProfile;
import org.bonitasoft.store.artefactdeploy.DeployStrategyResource;
import org.bonitasoft.store.artifact.Artifact.TypeArtifact;
import org.bonitasoft.store.toolbox.LoggerStore;

/**
 * create artefact
 */
public class FactoryArtifact {

    public static BEvent EVENT_NO_DETECTION = new BEvent(FactoryArtifact.class.getName(), 1, Level.APPLICATIONERROR, "This file does not match any artefact", "The artefact can't be deployed", "No detection", "Check the file");

    protected static BEvent EVENT_FAILED_DETECTION = new BEvent(FactoryArtifact.class.getName(), 2, Level.APPLICATIONERROR, "Error during detection", "An error is detected during the exploitation", "No detection", "Check the file");

    public static FactoryArtifact getInstance() {
        return new FactoryArtifact();
    }

    /**
     * create the artefact from a FileContent
     * 
     * @param fileName
     * @param fileContent
     * @param bonitaStore
     * @return
     */
    public static class ArtefactResult {

        public Artifact artefact;
        public String logAnalysis = "";
        public List<BEvent> listEvents = new ArrayList<BEvent>();
    }

    public ArtefactResult getInstanceArtefact(String fileName, File fileContent, BonitaStore bonitaStore, LoggerStore logStore) {
        ArtefactResult artefactResult = new ArtefactResult();

        artefactResult.logAnalysis = "analysis[" + fileName + "]";
        Date dateFile = null;
        try {
            // JDK 1.7
            dateFile = new Date(fileContent.lastModified());
            // JDK 1.8
            // BasicFileAttributes attributes = Files.readAttributes(fileContent.toPath(), BasicFileAttributes.class);
            // dateFile = new Date(attributes.lastModifiedTime().toMillis());

        } catch (Exception e) {
            artefactResult.listEvents.add(new BEvent(EVENT_FAILED_DETECTION, e, "Cannot Acess Date fileName[" + fileName + "]"));
            return artefactResult;
        }

        // ----------------------- process
        if (fileName.endsWith(".bar")) {

            int separator = fileName.indexOf("--");
            if (separator != -1) {
                String processName = fileName.substring(0, separator);

                String processVersion = fileName.substring(separator + 2);
                processVersion = processVersion.substring(0, processVersion.length() - 4); // remove
                // .bar
                artefactResult.logAnalysis += "Process[" + processName + "] version[" + processVersion + "] detected";
                artefactResult.artefact = new ArtifactProcess(processName, processVersion, "", dateFile, bonitaStore);
            }
            // XML file : profile
        } else if (fileName.endsWith(".xml")) {
            // <profile isDefault="false" name="testmyprofile">
            // so, do not use the XML Parse which take time, try a
            // direct approach reading the file
            try {
                String line = readLine(fileContent, 2);
                if (line != null && line.trim().startsWith("<profile ")) {
                    // this is a profile
                    int profileNamePos = line.indexOf("name=\"");

                    if (profileNamePos != -1) {
                        profileNamePos += "name=\"".length();
                        int endProfileName = line.indexOf("\"", profileNamePos);
                        String name = line.substring(profileNamePos, endProfileName);
                        artefactResult.logAnalysis += "profile[" + name + "] detected";

                        artefactResult.artefact = new ArtifactProfile(name, null, "", dateFile, bonitaStore);
                    }
                }
                if (line != null && line.trim().startsWith("<customUserInfoDefinitions")) {
                    // this is an organization
                    String name = fileName.substring(0, fileName.length() - ".xml".length());
                    artefactResult.artefact = new ArtifactOrganization(name, null, "", dateFile, bonitaStore);
                }
                if (line != null && line.trim().startsWith("<application")) {
                    String name = searchInXmlContent(fileContent, "displayName");
                    String description = searchInXmlContent(fileContent, "description");
                    artefactResult.logAnalysis += "Application[" + name + "] detected";

                    artefactResult.artefact = new ArtifactLivingApplication(name, null, "", dateFile, bonitaStore);
                }
            } catch (Exception e) {

            }

        } else if (fileName.endsWith(".zip")) {
            // ZIP file : may be a lot of thing !
            PropertiesAttribut propertiesAttribute = null;
            try {
                propertiesAttribute = searchInPagePropertie(fileContent);
                if (propertiesAttribute == null) {
                    artefactResult.listEvents.add(new BEvent(EVENT_FAILED_DETECTION, "propertiesAttribute is null fileName[" + fileName + "]"));
                    return artefactResult;
                }
            } catch (Exception e) {
                artefactResult.listEvents.add(new BEvent(EVENT_FAILED_DETECTION, e, "fileName[" + fileName + "]"));
                return artefactResult;
            }

            if (propertiesAttribute.contentType == null || "page".equals(propertiesAttribute.contentType)) {
                artefactResult.artefact = new ArtifactCustomPage(propertiesAttribute.name, propertiesAttribute.version, propertiesAttribute.description, dateFile, bonitaStore);

            } else if ("apiExtension".equalsIgnoreCase(propertiesAttribute.contentType)) {
                artefactResult.artefact = new ArtifactRestApi(propertiesAttribute.name, propertiesAttribute.version, propertiesAttribute.description, dateFile, bonitaStore);

            } else if ("layout".equalsIgnoreCase(propertiesAttribute.contentType)) {
                artefactResult.artefact = new ArtifactLayout(propertiesAttribute.name, propertiesAttribute.version, propertiesAttribute.description, dateFile, bonitaStore);

            } else if ("form".equalsIgnoreCase(propertiesAttribute.contentType)) {
                // a form : only possible to deploy it in a process,
                // so... we need the process
            } else if ("theme".equalsIgnoreCase(propertiesAttribute.contentType)) {
                artefactResult.artefact = new ArtifactTheme(propertiesAttribute.name, propertiesAttribute.version, propertiesAttribute.description, dateFile, bonitaStore);

            } else {
                logStore.severe("Unknow artefact contentType=[" + propertiesAttribute.contentType + "]");
            }
        } else if (fileName.endsWith(".groovy")) {
            String name = fileName.substring(0, fileName.length() - ".groovy".length());
            artefactResult.artefact = new ArtifactGroovy(name, null, "", dateFile, bonitaStore);

            // ----------------------- Nothing
        } else {
            artefactResult.logAnalysis += "No detection.";
            artefactResult.listEvents.add(new BEvent(EVENT_NO_DETECTION, "fileName[" + fileName + "]"));
        }
        if (artefactResult.artefact != null) {
            artefactResult.listEvents.addAll(artefactResult.artefact.loadFromFile(fileContent));

            applyStrategy(artefactResult.artefact);
        }
        return artefactResult;
    }

    /**
     * get a artefact from its type
     * 
     * @param type
     * @param name
     * @param version
     * @param description
     * @param dateCreation
     * @param bonitaStore
     * @return
     */

    public Artifact getFromType(TypeArtifact type, String name, String version, String description, Date dateCreation, BonitaStore bonitaStore) {

        Artifact artefact = null;
        if (type == TypeArtifact.BDM)
            artefact = new ArtifactBDM(name, version, description, dateCreation, bonitaStore);
        if (type == TypeArtifact.PROCESS)
            artefact = new ArtifactProcess(name, version, description, dateCreation, bonitaStore);
        if (type == TypeArtifact.CUSTOMPAGE)
            artefact = new ArtifactCustomPage(name, version, description, dateCreation, bonitaStore);
        if (type == TypeArtifact.CUSTOMWIDGET)
            artefact = null;
        if (type == TypeArtifact.LAYOUT)
            artefact = new ArtifactLayout(name, version, description, dateCreation, bonitaStore);
        if (type == TypeArtifact.LIVINGAPP)
            artefact = new ArtifactLivingApplication(name, version, description, dateCreation, bonitaStore);
        if (type == TypeArtifact.LOOKANDFEEL)
            artefact = new ArtifactLookAndFeel(name, version, description, dateCreation, bonitaStore);
        if (type == TypeArtifact.ORGANIZATION)
            artefact = new ArtifactLookAndFeel(name, version, description, dateCreation, bonitaStore);
        if (type == TypeArtifact.PROFILE)
            artefact = new ArtifactProfile(name, version, description, dateCreation, bonitaStore);
        if (type == TypeArtifact.RESTAPI)
            artefact = new ArtifactRestApi(name, version, description, dateCreation, bonitaStore);
        if (type == TypeArtifact.THEME)
            artefact = new ArtifactTheme(name, version, description, dateCreation, bonitaStore);
        if (type == TypeArtifact.GROOVY)
            artefact = new ArtifactGroovy(name, version, description, dateCreation, bonitaStore);

        if (artefact != null)
            applyStrategy(artefact);

        return artefact;
    }

    /*
     * apply all strategy in artefact
     */
    private void applyStrategy(Artifact artefact) {
        if (artefact instanceof ArtifactBDM)
            artefact.setDeployStrategy(new DeployStrategyBDM());
        if (artefact instanceof ArtifactProcess)
            artefact.setDeployStrategy(new DeployStrategyProcess());
        if (artefact instanceof ArtifactProfile)
            artefact.setDeployStrategy(new DeployStrategyProfile());
        if (artefact instanceof ArtifactCustomPage)
            artefact.setDeployStrategy(new DeployStrategyResource());
        if (artefact instanceof ArtifactLayout)
            artefact.setDeployStrategy(new DeployStrategyResource());
        if (artefact instanceof ArtifactLivingApplication)
            artefact.setDeployStrategy(new DeployStrategyLivingApplication());
        if (artefact instanceof ArtifactLookAndFeel)
            artefact.setDeployStrategy(new DeployStrategyLookAndFeel());
        if (artefact instanceof ArtifactOrganization)
            artefact.setDeployStrategy(new DeployStrategyOrganization());
        if (artefact instanceof ArtifactRestApi)
            artefact.setDeployStrategy(new DeployStrategyResource());
        if (artefact instanceof ArtifactTheme)
            artefact.setDeployStrategy(new DeployStrategyResource());

    }

    /**
     * read a line
     * 
     * @param file
     * @param lineToRead
     * @return
     */
    protected String readLine(File file, int lineToRead) throws Exception {
        BufferedReader br = null;
        FileReader fr = null;
        try {
            fr = new FileReader(file);

            br = new BufferedReader(fr);

            int lineNumber = 0;
            String sCurrentLine;

            while ((sCurrentLine = br.readLine()) != null) {
                lineNumber++;
                if (lineToRead == lineNumber)
                    return sCurrentLine;
            }
            return null;
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                if (br != null)
                    br.close();

                if (fr != null)
                    fr.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * search in the XML content and search the content of <Hello>this is
     * that</Hello> for a xmlTag "Hello"
     * 
     * @param file
     * @param xmlTag
     * @return
     * @throws Exception
     */
    private String searchInXmlContent(File file, String xmlTag) throws Exception {
        String content = readFileContent(file);
        int beginPos = content.indexOf("<" + xmlTag + ">");

        if (beginPos != -1) {

            int endProfileName = content.indexOf("</" + xmlTag, beginPos);
            if (endProfileName != -1)
                return content.substring(beginPos + xmlTag.length() + 2, endProfileName);
        }
        return null;
    }

    private class PropertiesAttribut {

        public String contentType;
        public String name;
        public String description;
        public String version;

    }

    /**
     * open a ZIP file, and search for the page.properties. Then, read inside
     * all the different properties
     * 
     * @param file
     * @return
     * @throws Exception
     */
    private PropertiesAttribut searchInPagePropertie(File file) throws Exception {
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new FileInputStream(file));
            // get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            String content = null;
            while (ze != null && content == null) {

                String fileName = ze.getName();
                if (fileName.equals("page.properties")) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int sizeEntry = (int) ze.getSize();
                    byte[] buffer = new byte[10000];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }

                    // byte[] bytes = new byte[sizeEntry];
                    // int sizeRead=zis.read(bytes, 0, bytes.length);
                    content = new String(out.toByteArray(), "UTF-8");
                }
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();
            zis = null;
            if (content == null)
                return null;

            Properties p = new Properties();
            p.load(new StringReader(content));
            PropertiesAttribut propertiesAttribut = new PropertiesAttribut();
            propertiesAttribut.name = p.getProperty("name");
            propertiesAttribut.version = p.getProperty("version");
            propertiesAttribut.description = p.getProperty("description");
            propertiesAttribut.contentType = p.getProperty("contentType");
            return propertiesAttribut;

        } catch (Exception e) {
            if (zis != null)
                zis.close();
            throw e;
        }
    }

    /**
     * @param file
     * @return
     * @throws Exception
     */
    private String readFileContent(File file) throws Exception {
        BufferedReader br = null;
        FileReader fr = null;
        StringBuffer content = new StringBuffer();
        try {
            fr = new FileReader(file);
            br = new BufferedReader(fr);

            String sCurrentLine;

            while ((sCurrentLine = br.readLine()) != null) {
                content.append(sCurrentLine + "\n");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                if (br != null)
                    br.close();

                if (fr != null)
                    fr.close();
            } catch (Exception e) {
            }
        }
        return content.toString();
    }

}
