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
import org.bonitasoft.store.artefactdeploy.DeployStrategy.DeployOperation;
import org.bonitasoft.store.toolbox.LoggerStore;


/* ******************************************************************************** */
/*                                                                                  */
/* BonitaAccessorClient : goal of this class is to be accessible in program, like maven plug in         */
/*                                                                                  */
/*                                                                                  */
/* ******************************************************************************** */


public class BonitaAccessorClient {

    
    private static BEvent EVENT_DEPLOY_FAILED = new BEvent(BonitaAccessorClient.class.getName(), 1, Level.APPLICATIONERROR, "Error during a deploy", "The artefact can't be deployed", "Artefact can't be deployed", "Check the exception");
 
    private APISession apiSession = null;
    private boolean alreadyLogged = false;

    public static BonitaAccessorClient getInstance( )
    {
        return new BonitaAccessorClient();
    }
    

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Login / logout                                                                   */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    
    /**
     * 
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

            /* if (newConnectMethod) {
                map.put("org.bonitasoft.engine.api-type.server.url",
                        applicationUrl == null ? "http://localhost:7080" : applicationUrl);
                map.put("org.bonitasoft.engine.api-type.application.name",
                        applicationName == null ? "bonita" : applicationName);

            } else {
            */
                map.put("server.url", applicationUrl == null ? "http://localhost:7080" : applicationUrl);
                map.put("application.name", applicationName == null ? "bonita" : applicationName);
            // }
            APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, map);

            // Set the username and password
            // final String username = "helen.kelly";
            final String username = "walter.bates";
            final String password = "bpm";

            // get the LoginAPI using the TenantAPIAccessor
           LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();

            // log in to the tenant to create a session
           apiSession = loginAPI.login(username, password);

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
    public boolean logout()  {
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

    public List<BEvent> deployArtefact( File fileArtefact)
    {
        try
        {
            System.out.println("Deploy Artefact ["+fileArtefact.getAbsolutePath()+"]");
            
            File pathDirectory = new File( fileArtefact.getAbsolutePath() );
            BonitaStoreAPI bonitaStoreAPI = BonitaStoreAPI.getInstance();
            BonitaStore bonitaStore = bonitaStoreAPI.getDirectoryStore( pathDirectory);
            
            FactoryArtefact factoryArtefact = FactoryArtefact.getInstance();
            LoggerStore loggerStore = new LoggerStore();
            
            BonitaAccessor BonitaAccessor = new BonitaAccessor( apiSession );
            
            System.out.println("Load Artefact");
            ArtefactResult artefact = factoryArtefact.getInstanceArtefact(fileArtefact.getName(), fileArtefact, bonitaStore, loggerStore);
            if (BEventFactory.isError( artefact.listEvents)) {
                System.out.println("Load error "+ artefact.listEvents.toString());
                return artefact.listEvents;
            }
            
            System.out.println("Deploy Artefact");
            DeployOperation deployOperation = artefact.artefact.deploy(BonitaAccessor, loggerStore);

            System.out.println("Deploiment "+ deployOperation.listEvents.toString());

            return deployOperation.listEvents;
        }
        catch (Exception e)
        {
            List<BEvent> listEvents = new ArrayList<BEvent>();
            listEvents.add( new BEvent(EVENT_DEPLOY_FAILED,  e,"During deploy["+fileArtefact.getName()+"]"));
            return listEvents;
        }
                
    }
}
