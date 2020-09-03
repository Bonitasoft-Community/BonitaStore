package org.bonitasoft.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.platform.LoginException;
import org.bonitasoft.engine.platform.LogoutException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.session.SessionNotFoundException;
import org.bonitasoft.engine.util.APITypeManager;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.store.artifact.ArtifactCustomPage;
import org.bonitasoft.store.artifact.ArtifactProfile;
import org.bonitasoft.store.artifact.FactoryArtifact;
import org.bonitasoft.store.InputArtifact.BonitaStoreInputFile;
import org.bonitasoft.store.artifact.Artifact.TypeArtifact;
import org.bonitasoft.store.artifact.FactoryArtifact.ArtifactResult;
import org.bonitasoft.store.artifactdeploy.DeployStrategy;
import org.bonitasoft.store.artifactdeploy.DeployStrategy.DeployOperation;
import org.bonitasoft.store.artifactdeploy.DeployStrategy.DetectionStatus;
import org.bonitasoft.store.artifactdeploy.DeployStrategy.UPDATE_STRATEGY;
import org.bonitasoft.store.toolbox.LoggerStore;

/* ******************************************************************************** */
/*                                                                                  */
/* BonitaAccessorClient : goal of this class is to be accessible in program, like maven plug in */
/*                                                                                  */
/*                                                                                  */
/* ******************************************************************************** */

public class BonitaStoreAccessorClient {

    private final static BEvent EVENT_DEPLOY_FAILED = new BEvent(BonitaStoreAccessorClient.class.getName(), 1, Level.APPLICATIONERROR, "Error during a deploy", "The artefact can't be deployed", "Artefact can't be deployed", "Check the exception");

    private APISession apiSession = null;
    private boolean alreadyLogged = false;

    public static BonitaStoreAccessorClient getInstance() {
        return new BonitaStoreAccessorClient();
    }

    public static void main(final String[] args) {
        String applicationUrl = args.length > 0 ? args[0] : null;
        String applicationName = args.length > 1 ? args[1] : null;
        String userName = args.length > 2 ? args[2] : null;
        String passwd = args.length > 3 ? args[3] : null;
        String fileName = args.length > 4 ? args[4] : null;
        List<String> listOptions = new ArrayList<String>();
        for (int i = 5; i < args.length; i++)
            listOptions.add(args[i]);
        // decode options
        UPDATE_STRATEGY strategy = UPDATE_STRATEGY.UPDATE;
        String insertIntoProfile = null;
        for (String option : listOptions) {
            StringTokenizer st = new StringTokenizer(option, ":");
            String command = st.hasMoreTokens() ? st.nextToken() : "";
            String value = st.hasMoreTokens() ? st.nextToken() : "";

            if ("strategy".equalsIgnoreCase(command)) {
                try {
                    strategy = UPDATE_STRATEGY.valueOf(value);
                } catch (Exception e) {
                    System.out.println("UpdateStrategy [" + value + "] unknow, only " + UPDATE_STRATEGY.UPDATE + "," + UPDATE_STRATEGY.DELETEANDADD + " accepted");
                }
            }
            if ("profile".equalsIgnoreCase(command)) {
                insertIntoProfile = value;
            }
        }
        System.out.println("BonitaStoreClient: Start Connection[" + applicationUrl + "/" + applicationName + "] User[" + userName + "] password[" + passwd + "] FileToDeploy[" + fileName + "]");
        BonitaStoreAccessorClient bonitaAccessorClient = BonitaStoreAccessorClient.getInstance();

        System.out.print("BonitaStoreClient: Login...");

        boolean isConnected = bonitaAccessorClient.login(applicationUrl, applicationName, userName, passwd);
        if (!isConnected) {
            System.out.println("FAILED");
            return;
        }
        System.out.println("SUCCESS");

        System.out.println("BonitaStoreClient: Deploy...");
        File fileArtefact = new File(fileName);
        // Copy the file
        // backupFile(fileArtefact);
        DeployOperation deploy = bonitaAccessorClient.deployArtifact(fileArtefact, strategy);
        if (BEventFactory.isError(deploy.listEvents)) {
            System.out.println("FAILED " + deploy.listEvents.toString());
        } else {

            System.out.println("SUCCESS");

            if (insertIntoProfile != null) {
                ArtifactProfile profileBo = bonitaAccessorClient.getOrCreateProfile(insertIntoProfile);
                if (profileBo != null) {
                    List<BEvent> listEvents = bonitaAccessorClient.registerInProfile(profileBo, (ArtifactCustomPage) deploy.artifact);
                    System.out.println("Registration:" + listEvents);
                }

            }
        }

        bonitaAccessorClient.logout();
    }
    /* ******************************************************************************** */
    /*                                                                                  */
    /* Login / logout */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    /**
     * @param applicationUrl
     * @param applicationName
     * @param userName
     * @param passwd
     * @return
     */
    public boolean login(final String applicationUrl, final String applicationName, final String userName,
            final String passwd) {

        try {
            // Define the REST parameters
            final Map<String, String> map = new HashMap<String, String>();

            map.put("server.url", applicationUrl == null ? "http://localhost:7080" : applicationUrl);
            map.put("application.name", applicationName == null ? "bonita" : applicationName);

            APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, map);

            // Set the username and password
            // final String username = "helen.kelly";

            // get the LoginAPI using the TenantAPIAccessor
            LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();

            // log in to the tenant to create a session
            apiSession = loginAPI.login(userName, passwd);

            alreadyLogged = true;
            return true;
        } catch (final BonitaHomeNotSetException e) {
            // e.printStackTrace();
            return false;
        } catch (final ServerAPIException e) {
            // e.printStackTrace();
            return false;
        } catch (final UnknownAPITypeException e) {
            // e.printStackTrace();
            return false;
        } catch (final LoginException e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * logout
     *
     * @return
     */
    public boolean logout() {
        try {
            LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
            loginAPI.logout(apiSession);
            alreadyLogged = false;
            return true;
        } catch (final SessionNotFoundException e) {

            return false;
        } catch (final LogoutException e) {

            return false;
        } catch (final Exception e) {
            return false;
        }
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Strategy operation : deploy */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    public DeployOperation deployArtifact(File fileArtifact, UPDATE_STRATEGY strategy) {
        try {
            System.out.println("Start Deploying Artefact [" + fileArtifact.getAbsolutePath() + "]");

            File pathDirectory = new File(fileArtifact.getAbsolutePath());
            BonitaStoreAPI bonitaStoreAPI = BonitaStoreAPI.getInstance();
            BonitaStore bonitaStore = bonitaStoreAPI.getDirectoryStore(pathDirectory,true);

            FactoryArtifact factoryArtefact = FactoryArtifact.getInstance();
            LoggerStore loggerStore = new LoggerStore();

            BonitaStoreAccessor BonitaAccessor = new BonitaStoreAccessor(apiSession);

            System.out.println("  Load Artefact");
            ArtifactResult artefactResult = factoryArtefact.getInstanceArtefact(fileArtifact.getName(), new BonitaStoreInputFile( fileArtifact), true, bonitaStore, loggerStore);
            if (BEventFactory.isError(artefactResult.listEvents)) {
                System.out.println("Load error " + artefactResult.listEvents.toString());
                DeployOperation deployOperation = new DeployOperation();
                deployOperation.artifact = artefactResult.artifact;
                deployOperation.listEvents = artefactResult.listEvents;
                return deployOperation;
            }

            System.out.println("  Deploy Artefact");

            // update the deploy Strategy
            DeployStrategy deployStrategy = artefactResult.artifact.getDeployStrategy();
            deployStrategy.setUpdateStrategy(strategy);
            artefactResult.artifact.setDeployStrategy(deployStrategy);

            // then deploy
            DeployOperation deployOperation = artefactResult.artifact.deploy(new BonitaStoreParameters(), BonitaAccessor, loggerStore);
            deployOperation.artifact = artefactResult.artifact;
            System.out.println("Deploiment Status:" + deployOperation.deploymentStatus.toString());
            System.out.println("Deploiment Details:" + deployOperation.listEvents.toString());

            return deployOperation;
        } catch (Exception e) {
            DeployOperation deploy = new DeployOperation();
            deploy.listEvents.add(new BEvent(EVENT_DEPLOY_FAILED, e, "During deploy[" + fileArtifact.getName() + "]"));
            return deploy;
        }

    }
    /* ******************************************************************************** */
    /*                                                                                  */
    /* Profile operation */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    private String profileTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<profiles:profiles xmlns:profiles=\"http://www.bonitasoft.org/ns/profile/6.1\"> "
            + "   <profile name=\"@@PROFILENAME@@\" isDefault=\"false\"> "
            + "        <description></description>"
            + "        <profileEntries>"
            + "        </profileEntries>"
            + "        <profileMapping>"
            + "            <users/>"
            + "            <groups/>"
            + "            <memberships/>"
            + "            <roles>"
            + "                <role>member</role>"
            + "            </roles>"
            + "        </profileMapping>"
            + "    </profile>"
            + "</profiles:profiles>";

    public ArtifactProfile getOrCreateProfile(String profileName) {

        BonitaStoreAPI bonitaStoreAPI = BonitaStoreAPI.getInstance();
        BonitaStore bonitaStore = bonitaStoreAPI.getLocalStore(apiSession);

        FactoryArtifact factoryArtefact = FactoryArtifact.getInstance();
        LoggerStore loggerStore = new LoggerStore();

        BonitaStoreAccessor bonitaAccessor = new BonitaStoreAccessor(apiSession);

        System.out.println("  Load Artefact Profile[" + profileName + "]");
        ArtifactProfile artefactProfileBO = (ArtifactProfile) factoryArtefact.getFromType(TypeArtifact.PROFILE, "BOTools", "1.0", "Profile to access Custom page", new Date(), bonitaStore);
        String profileContent = profileTemplate.replace("@@PROFILENAME@@", profileName);

        artefactProfileBO.loadFromString(profileContent);
        DeployOperation deploy = artefactProfileBO.detectDeployment(new BonitaStoreParameters(), bonitaAccessor, loggerStore);
        if (deploy.detectionStatus == DetectionStatus.NEWARTEFAC) {
            deploy = artefactProfileBO.deploy(new BonitaStoreParameters(), bonitaAccessor, loggerStore);
            if (BEventFactory.isError(deploy.listEvents)) {
                System.out.println("CreateProfile [" + profileName + "] failed " + deploy.listEvents.toString());
                return null;
            }
        }
        return artefactProfileBO;

    }

    public List<BEvent> registerInProfile(ArtifactProfile profile, ArtifactCustomPage page) {
        BonitaStoreAccessor bonitaAccessor = new BonitaStoreAccessor(apiSession);

        return profile.registerCustomPage(page, bonitaAccessor);
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /*
     * private operation
     * /*
     */
    /*                                                                                  */
    /* ******************************************************************************** */

    private static void backupFile(File sourceFile) {

        InputStream inStream = null;
        OutputStream outStream = null;

        try {
            System.out.print("Backup file["+sourceFile.getName()+"] to ["+sourceFile.getAbsoluteFile() + "_bak.zip]");
            File bfile = new File(sourceFile.getAbsoluteFile() + "_bak.zip");

            inStream = new FileInputStream(sourceFile);
            outStream = new FileOutputStream(bfile);

            byte[] buffer = new byte[1024];

            int length;
            //copy the file content in bytes 
            while ((length = inStream.read(buffer)) > 0) {

                outStream.write(buffer, 0, length);

            }

            inStream.close();
            outStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
