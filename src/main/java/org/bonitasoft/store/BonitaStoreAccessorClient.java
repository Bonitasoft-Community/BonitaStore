package org.bonitasoft.store;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.artefact.ArtefactCustomPage;
import org.bonitasoft.store.artefact.FactoryArtefact;
import org.bonitasoft.store.artefact.FactoryArtefact.ArtefactResult;
import org.bonitasoft.store.artefactdeploy.DeployStrategy;
import org.bonitasoft.store.artefactdeploy.DeployStrategy.DeployOperation;
import org.bonitasoft.store.artefactdeploy.DeployStrategy.UPDATE_STRATEGY;
import org.bonitasoft.store.toolbox.LoggerStore;

/* ******************************************************************************** */
/*                                                                                  */
/* BonitaAccessorClient : goal of this class is to be accessible in program, like maven plug in */
/*                                                                                  */
/*                                                                                  */
/* ******************************************************************************** */

public class BonitaStoreAccessorClient {

    private static BEvent EVENT_DEPLOY_FAILED = new BEvent(BonitaStoreAccessorClient.class.getName(), 1, Level.APPLICATIONERROR, "Error during a deploy", "The artefact can't be deployed", "Artefact can't be deployed", "Check the exception");

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
        String strategySt = args.length > 5 ? args[5] : null;
        if (strategySt==null)
            strategySt="UPDATE";
        UPDATE_STRATEGY strategy = UPDATE_STRATEGY.UPDATE; 
        try
        {
            strategy=UPDATE_STRATEGY.valueOf( strategySt);
        }
        catch(Exception e)
        {
            System.out.println("UpdateStrategy ["+strategySt+"] unknow, only "+UPDATE_STRATEGY.UPDATE+","+UPDATE_STRATEGY.DELETEANDADD+" accepted");    
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
        List<BEvent> listEvents = bonitaAccessorClient.deployArtefact(fileArtefact, strategy);
        if (BEventFactory.isError(listEvents)) {
            System.out.println("FAILED " + listEvents.toString());
        } else
            System.out.println("SUCCESS");

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
            e.printStackTrace();
            return false;
        } catch (final ServerAPIException e) {
            e.printStackTrace();
            return false;
        } catch (final UnknownAPITypeException e) {
            e.printStackTrace();
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

    public List<BEvent> deployArtefact(File fileArtefact, UPDATE_STRATEGY strategy) {
        try {
            System.out.println("Start Deploying Artefact [" + fileArtefact.getAbsolutePath() + "]");

            File pathDirectory = new File(fileArtefact.getAbsolutePath());
            BonitaStoreAPI bonitaStoreAPI = BonitaStoreAPI.getInstance();
            BonitaStore bonitaStore = bonitaStoreAPI.getDirectoryStore(pathDirectory);

            FactoryArtefact factoryArtefact = FactoryArtefact.getInstance();
            LoggerStore loggerStore = new LoggerStore();

            BonitaStoreAccessor BonitaAccessor = new BonitaStoreAccessor(apiSession);

            System.out.println("  Load Artefact");
            ArtefactResult artefact = factoryArtefact.getInstanceArtefact(fileArtefact.getName(), fileArtefact, bonitaStore, loggerStore);
            if (BEventFactory.isError(artefact.listEvents)) {
                System.out.println("Load error " + artefact.listEvents.toString());
                return artefact.listEvents;
            }

            System.out.println("  Deploy Artefact");

            // update the deploy Strategy
            DeployStrategy deployStrategy = artefact.artefact.getDeployStrategy();
            deployStrategy.setUpdateStrategy(strategy);
            artefact.artefact.setDeployStrategy(deployStrategy);
            
            // then deploy
            DeployOperation deployOperation = artefact.artefact.deploy(BonitaAccessor, loggerStore);

            System.out.println("Deploiment Status:" + deployOperation.deploymentStatus.toString());
            System.out.println("Deploiment nDetails:" + deployOperation.listEvents.toString());

            return deployOperation.listEvents;
        } catch (Exception e) {
            List<BEvent> listEvents = new ArrayList<BEvent>();
            listEvents.add(new BEvent(EVENT_DEPLOY_FAILED, e, "During deploy[" + fileArtefact.getName() + "]"));
            return listEvents;
        }

    }
}
