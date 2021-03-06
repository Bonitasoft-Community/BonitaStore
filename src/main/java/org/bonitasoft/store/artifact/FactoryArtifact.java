package org.bonitasoft.store.artifact;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bonitasoft.engine.page.ContentType;
import org.bonitasoft.engine.page.Page;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStore;
import org.bonitasoft.store.InputArtifact.BonitaStoreInput;
import org.bonitasoft.store.artifact.Artifact.TypeArtifact;
import org.bonitasoft.store.artifactdeploy.DeployStrategyBDM;
import org.bonitasoft.store.artifactdeploy.DeployStrategyLivingApplication;
import org.bonitasoft.store.artifactdeploy.DeployStrategyLookAndFeel;
import org.bonitasoft.store.artifactdeploy.DeployStrategyOrganization;
import org.bonitasoft.store.artifactdeploy.DeployStrategyProcess;
import org.bonitasoft.store.artifactdeploy.DeployStrategyProfile;
import org.bonitasoft.store.artifactdeploy.DeployStrategyResource;
import org.bonitasoft.store.toolbox.LoggerStore;

/**
 * create artefact
 */
public class FactoryArtifact {

    public final static BEvent EVENT_NO_DETECTION = new BEvent(FactoryArtifact.class.getName(), 1, Level.APPLICATIONERROR, "This file does not match any artefact", "The artefact can't be deployed", "No detection", "Check the file");

    protected final static BEvent EVENT_FAILED_DETECTION = new BEvent(FactoryArtifact.class.getName(), 2, Level.APPLICATIONERROR, "Error during detection", "An error is detected during the exploitation", "No detection", "Check the file");

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
    public static class ArtifactResult {

        public Artifact artifact;
        public StringBuilder logAnalysis = new StringBuilder();
        public List<BEvent> listEvents = new ArrayList<>();
    }

    public ArtifactResult getInstanceArtefact(String fileName, BonitaStoreInput fileContent, boolean loadArtifact, BonitaStore bonitaStore, LoggerStore logStore) {
        ArtifactResult artefactResult = new ArtifactResult();

        artefactResult.logAnalysis.append("analysis File[" + fileName + "]");
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
                // remove .bar
                processVersion = processVersion.substring(0, processVersion.length() - 4);
                processVersion = processVersion.trim();
                artefactResult.logAnalysis.append("Process[" + processName + "] version[" + processVersion + "] detected from filename;");
                // Note: this name / version may be completely wrong actually, there is no way to detect it (no way to load the business Archive and ask the name....)
                artefactResult.artifact = new ArtifactProcess(processName, processVersion, "", dateFile, dateFile, bonitaStore);
            }
            // XML file : profile
        } else if (fileName.endsWith(".xml")) {
            // <profile isDefault="false" name="testmyprofile">
            // so, do not use the XML Parse which take time, try a
            // direct approach reading the file
            try {
                String content = fileContent.getContentText();
                if (content == null)
                    return artefactResult;
                // remove the first <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                if (content.startsWith("<?xml")) {
                    int pos = content.indexOf("?>");
                    if (pos != -1)
                        content = content.substring(pos + 2 + 1);
                }
                if (content.startsWith("<profile ")) {
                    // this is a profile
                    int profileNamePos = content.indexOf("name=\"");

                    if (profileNamePos != -1) {
                        profileNamePos += "name=\"".length();
                        int endProfileName = content.indexOf('\"', profileNamePos);
                        String name = content.substring(profileNamePos, endProfileName);
                        artefactResult.logAnalysis.append("Profile[" + name + "] detected from XML[" + fileName + "];");

                        artefactResult.artifact = new ArtifactProfile(name, null, "", dateFile, dateFile, bonitaStore);
                    }
                }
                if (content.startsWith("<customUserInfoDefinitions")) {
                    // this is an organization
                    String name = fileName.substring(0, fileName.length() - ".xml".length());
                    artefactResult.logAnalysis.append("Organisation[" + name + "] detected from XML[" + fileName + "];");
                    artefactResult.artifact = new ArtifactOrganization(name, null, "", dateFile, dateFile, bonitaStore);
                }
                if (content.startsWith("<application")) {
                    // we may have MULTIPLE application, so the first one is important
                    String name = searchInXmlContent(content, "displayName");
                    String description = searchInXmlContent(content, "description");
                    artefactResult.logAnalysis.append("Application[" + name + "] detected from XML[" + fileName + "];");

                    artefactResult.artifact = new ArtifactLivingApplication(name, null, description, dateFile, dateFile, bonitaStore);
                }
                if (content.startsWith("<organization")) {
                    String name = fileName;
                    if (name.endsWith(".xml"))
                        name = name.substring(0, name.length() - ".xml".length());
                    String description = "organization";
                    artefactResult.logAnalysis.append("Organisation[" + name + "] detected from XML[" + fileName + "];");

                    artefactResult.artifact = new ArtifactOrganization(name, null, description, dateFile, dateFile, bonitaStore);
                }

            } catch (Exception e) {
                // ignore error here, just we can't manage the detection                
            }

        } else if (fileName.endsWith(".zip") && !fileName.equals("bdm.zip")) {
            try {
                InputStream contentStream = fileContent.getContentInputStream();

                // ZIP file : may be a lot of thing !
                PropertiesAttribut propertiesAttribute = null;
                try {
                    propertiesAttribute = searchInPagePropertie(contentStream);
                    if (propertiesAttribute == null) {
                        artefactResult.listEvents.add(new BEvent(EVENT_FAILED_DETECTION, "propertiesAttribute is null fileName[" + fileName + "]"));
                        return artefactResult;
                    }
                } catch (Exception e) {
                    artefactResult.listEvents.add(new BEvent(EVENT_FAILED_DETECTION, e, "fileName[" + fileName + "]"));
                    return artefactResult;
                }

                if (propertiesAttribute.contentType == null || "page".equals(propertiesAttribute.contentType)) {
                    artefactResult.logAnalysis.append("CustomPage[" + propertiesAttribute.name + "] detected from Properties[" + fileName + "];");
                    artefactResult.artifact = new ArtifactCustomPage(propertiesAttribute.name, propertiesAttribute.version, propertiesAttribute.description, dateFile, dateFile, bonitaStore);

                } else if ("apiExtension".equalsIgnoreCase(propertiesAttribute.contentType)) {
                    artefactResult.logAnalysis.append("RestAPIExtention[" + propertiesAttribute.name + "] detected from Properties[" + fileName + "];");
                    artefactResult.artifact = new ArtifactRestApi(propertiesAttribute.name, propertiesAttribute.version, propertiesAttribute.description, dateFile, dateFile, bonitaStore);

                } else if ("layout".equalsIgnoreCase(propertiesAttribute.contentType)) {
                    artefactResult.logAnalysis.append("Layout[" + propertiesAttribute.name + "] detected from Properties[" + fileName + "];");
                    artefactResult.artifact = new ArtifactLayout(propertiesAttribute.name, propertiesAttribute.version, propertiesAttribute.description, dateFile, dateFile, bonitaStore);

                } else if ("form".equalsIgnoreCase(propertiesAttribute.contentType)) {
                    // a form : only possible to deploy it in a process,
                    // so... we need the process
                } else if ("theme".equalsIgnoreCase(propertiesAttribute.contentType)) {
                    artefactResult.logAnalysis.append("Theme[" + propertiesAttribute.name + "] detected from Properties[" + fileName + "];");
                    artefactResult.artifact = new ArtifactTheme(propertiesAttribute.name, propertiesAttribute.version, propertiesAttribute.description, dateFile, dateFile, bonitaStore);

                } else {
                    artefactResult.logAnalysis.append("Unkown Artifact ContentType[" + propertiesAttribute.contentType + "] detected from Properties[" + fileName + "];");
                    logStore.severe("Unknow artifact contentType=[" + propertiesAttribute.contentType + "]");
                }
            } catch (Exception e) {
                artefactResult.listEvents.add(new BEvent(EVENT_FAILED_DETECTION, e, ""));
            }
        } else if (fileName.endsWith(".groovy")) {
            String name = fileName.substring(0, fileName.length() - ".groovy".length());
            artefactResult.logAnalysis.append("Groovy detected[" + name + "] detected from Properties[" + fileName + "];");
            artefactResult.artifact = new ArtifactGroovy(name, null, "", dateFile, dateFile, bonitaStore);

            // ----------------------- Nothing
        } else {
            artefactResult.logAnalysis.append("No detection avaiable for fileName[" + fileName + "]");
            artefactResult.listEvents.add(new BEvent(EVENT_NO_DETECTION, "fileName[" + fileName + "]"));
        }
        if (artefactResult.artifact != null && loadArtifact) {
            try {
                InputStream contentStream = fileContent.getContentInputStream();
                artefactResult.listEvents.addAll(artefactResult.artifact.loadFromInputStream(contentStream));
            } catch (Exception e) {
                artefactResult.listEvents.add(new BEvent(EVENT_FAILED_DETECTION, e, ""));
            }
        }
        applyStrategy(artefactResult.artifact);
        if (artefactResult.artifact != null)
            artefactResult.artifact.signature = fileContent.getSignature();

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

    public Artifact getFromType(TypeArtifact type, String name, String version, String description, Date dateCreation, Date dateVersion, BonitaStore bonitaStore) {

        Artifact artefact = null;
        if (type == TypeArtifact.BDM)
            artefact = new ArtifactBDM(name, version, description, dateCreation, dateVersion, bonitaStore);
        if (type == TypeArtifact.PROCESS)
            artefact = new ArtifactProcess(name, version, description, dateCreation, dateVersion, bonitaStore);
        if (type == TypeArtifact.CUSTOMPAGE)
            artefact = new ArtifactCustomPage(name, version, description, dateCreation, dateVersion, bonitaStore);
        if (type == TypeArtifact.CUSTOMWIDGET)
            artefact = null;
        if (type == TypeArtifact.LAYOUT)
            artefact = new ArtifactLayout(name, version, description, dateCreation, dateVersion, bonitaStore);
        if (type == TypeArtifact.LIVINGAPP)
            artefact = new ArtifactLivingApplication(name, version, description, dateCreation, dateVersion, bonitaStore);
        if (type == TypeArtifact.LOOKANDFEEL)
            artefact = new ArtifactLookAndFeel(name, version, description, dateCreation, dateVersion, bonitaStore);
        if (type == TypeArtifact.ORGANIZATION)
            artefact = new ArtifactLookAndFeel(name, version, description, dateCreation, dateVersion, bonitaStore);
        if (type == TypeArtifact.PROFILE)
            artefact = new ArtifactProfile(name, version, description, dateCreation, dateVersion, bonitaStore);
        if (type == TypeArtifact.RESTAPI)
            artefact = new ArtifactRestApi(name, version, description, dateCreation, dateVersion, bonitaStore);
        if (type == TypeArtifact.THEME)
            artefact = new ArtifactTheme(name, version, description, dateCreation, dateVersion, bonitaStore);
        if (type == TypeArtifact.GROOVY)
            artefact = new ArtifactGroovy(name, version, description, dateCreation, dateVersion, bonitaStore);

        if (artefact != null)
            applyStrategy(artefact);

        return artefact;
    }

    /**
     * get the artifact for an existing page
     * 
     * @param page
     * @param bonitaStore
     * @return
     */
    public Artifact getFromPage(Page page, BonitaStore bonitaStore) {
        Artifact artefact = null;
        if (ContentType.API_EXTENSION.equals(page.getContentType()))
            artefact = new ArtifactRestApi(page, bonitaStore);
        else if (ContentType.FORM.equals(page.getContentType()))
            artefact = new ArtifactCustomPage(page, bonitaStore);
        else if (ContentType.LAYOUT.equals(page.getContentType()))
            artefact = new ArtifactLayout(page, bonitaStore);
        else if (ContentType.PAGE.equals(page.getContentType()))
            artefact = new ArtifactCustomPage(page, bonitaStore);
        else if (ContentType.THEME.equals(page.getContentType()))
            artefact = new ArtifactTheme(page, bonitaStore);

        if (artefact != null)
            applyStrategy(artefact);

        return artefact;

    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* read line */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

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
                // this is the close, ignore exception
            }
        }
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Private */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    /*
     * apply all strategy in artifact
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

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Tools for detection */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    /**
     * search in the XML content and search the content of <Hello>this is
     * that</Hello> for a xmlTag "Hello"
     * 
     * @param file
     * @param xmlTag
     * @return
     * @throws Exception
     */
    private String searchInXmlContent(String content, String xmlTag) throws Exception {
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
    private PropertiesAttribut searchInPagePropertie(InputStream inputStream) throws Exception {

        try (ZipInputStream zis = new ZipInputStream(inputStream)) {

            // get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            String content = null;
            while (ze != null && content == null) {

                String fileName = ze.getName();
                if (fileName.equals("page.properties")) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    // int sizeEntry = (int) ze.getSize();
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
            throw e;
        }
    }

    /**
     * @param file
     * @return
     * @throws Exception
     *         private String readFileContent(File file) throws Exception {
     *         BufferedReader br = null;
     *         FileReader fr = null;
     *         StringBuffer content = new StringBuffer();
     *         try {
     *         fr = new FileReader(file);
     *         br = new BufferedReader(fr);
     *         String sCurrentLine;
     *         while ((sCurrentLine = br.readLine()) != null) {
     *         content.append(sCurrentLine + "\n");
     *         }
     *         } catch (Exception e) {
     *         throw e;
     *         } finally {
     *         try {
     *         if (br != null)
     *         br.close();
     *         if (fr != null)
     *         fr.close();
     *         } catch (Exception e) {
     *         }
     *         }
     *         return content.toString();
     *         }
     */
}
