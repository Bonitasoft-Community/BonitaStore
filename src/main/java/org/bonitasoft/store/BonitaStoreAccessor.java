package org.bonitasoft.store;

import java.lang.reflect.Method;

import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.ApplicationAPI;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.OrganizationAPI;
import org.bonitasoft.engine.api.PageAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;

public class BonitaStoreAccessor {

    /**
     * the apiAccessor is not available everytime in the same way : build a new
     * one
     */
    public APISession apiSession;
    public ProcessAPI processAPI;
    public IdentityAPI identityAPI;
    public ProfileAPI profileAPI;
    public PageAPI pageAPI;
    public ApplicationAPI applicationAPI;
    public OrganizationAPI organisationAPI;

    /**
     * getinstance to be consistance with other API factory
     * 
     * @param apiSession
     * @return
     */
    public static BonitaStoreAccessor getInstance(APISession apiSession) {
        return new BonitaStoreAccessor(apiSession);
    }
    public static BonitaStoreAccessor getInstance(APIAccessor apiAccessor) {
        return new BonitaStoreAccessor(apiAccessor);
    }

    public BonitaStoreAccessor(APISession apiSession) {
        this.apiSession = apiSession;
        try {
            this.processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
            this.identityAPI = TenantAPIAccessor.getIdentityAPI(apiSession);
            this.profileAPI = TenantAPIAccessor.getProfileAPI(apiSession);
            this.pageAPI = TenantAPIAccessor.getCustomPageAPI(apiSession);
            this.applicationAPI = TenantAPIAccessor.getLivingApplicationAPI(apiSession);
            this.organisationAPI = TenantAPIAccessor.getIdentityAPI(apiSession);

        } catch (Exception e) {
        }
    }
    
    public BonitaStoreAccessor(APIAccessor apiAccessor) {
        try {
            this.processAPI = apiAccessor.getProcessAPI();
            this.identityAPI = apiAccessor.getIdentityAPI();
            this.profileAPI = apiAccessor.getProfileAPI();
            this.pageAPI = apiAccessor.getCustomPageAPI();
            this.applicationAPI = apiAccessor.getLivingApplicationAPI();
            this.organisationAPI = apiAccessor.getIdentityAPI();

        } catch (Exception e) {
        }
    }

    public ProfileAPI getProfileAPI() {
        ProfileAPI profile = null;
        try {
            Class<?> clazz = Class.forName("com.bonitasoft.engine.api.TenantAPIAccessor");
            if (clazz != null) {
                Method method = clazz.getMethod("getProfileAPI", APISession.class);
                profile = (ProfileAPI) method.invoke(null, apiSession);
                return profile;
            }

            return profile;
        } catch (Exception e) {
        }
        // Here ? Not a server API
        try {
            return TenantAPIAccessor.getProfileAPI(apiSession);
        } catch (Exception e) {
            return null;
        }

    }

}
