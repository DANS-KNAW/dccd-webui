/*******************************************************************************
 * Copyright 2015 DANS - Data Archiving and Networked Services
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package nl.knaw.dans.dccd.web;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

import nl.knaw.dans.common.ldap.management.ApacheDSServerBuilder;
import nl.knaw.dans.common.wicket.DebugStringResourceLoader;
import nl.knaw.dans.common.wicket.components.upload.EasyUploadProcesses;
import nl.knaw.dans.common.wicket.components.upload.EasyUploadWebRequest;
import nl.knaw.dans.common.wicket.components.upload.command.EasyUploadCancelCommand;
import nl.knaw.dans.common.wicket.components.upload.command.EasyUploadStatusCommand;
import nl.knaw.dans.dccd.application.services.DccdUserService;
import nl.knaw.dans.dccd.application.services.UserServiceException;
import nl.knaw.dans.dccd.web.authn.MemberListPage;
import nl.knaw.dans.dccd.web.authn.OrganisationListPage;
import nl.knaw.dans.dccd.web.error.InternalErrorPage;
import nl.knaw.dans.dccd.web.project.ProjectViewPage;
import nl.knaw.dans.dccd.web.search.pages.AdvSearchPage;
import nl.knaw.dans.dccd.web.search.pages.MyProjectsSearchResultPage;
import nl.knaw.dans.dccd.web.search.pages.PublicSearchResultPage;
import nl.knaw.dans.dccd.web.upload.CombinedUploadStatusCommand;
import nl.knaw.dans.dccd.web.upload.UploadFilesPage;

import org.apache.log4j.Logger;
import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.request.target.coding.MixedParamUrlCodingStrategy;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.apache.wicket.settings.IApplicationSettings;
import org.apache.wicket.util.lang.Bytes;
	
/**
 * Application object for your web application. If you want to run this application without deploying, run the Start class.
 *
 * @see nl.knaw.dans.dccd.web.Start#main(String[])
 */
public final class WicketApplication extends WebApplication
{
	private static Logger logger = Logger.getLogger(WicketApplication.class);

    /**
     * Constructor
     */
	public WicketApplication()
	{
	}

	/**
	 * @see org.apache.wicket.Application#getHomePage()
	 */
	@Override
	public Class<HomePage> getHomePage()
	{
		return HomePage.class;
	}

	@Override
	protected void init() 
	{	
		try
		{
			DccdUserService.getService().createInitialAdminUser();
		}
		catch (UserServiceException e)
		{
			logger.warn("Failed to create initial LDAP users", e);
		}
		
	    IApplicationSettings settings = getApplicationSettings();

	    // Note: maybe get it from the dccd configuration settings file
	    settings.setDefaultMaximumUploadSize(Bytes.gigabytes(1));

	    //TODO this doesn't work anymore, needs debugging!
	    List<IStringResourceLoader> rloaders = getResourceSettings().getStringResourceLoaders();
	    getResourceSettings().addStringResourceLoader(
	    		//new DebugStringResourceLoader(rloaders, "&lt;", "&gt", Locale.ENGLISH)
	    		new DebugStringResourceLoader(rloaders, "{", "}", Locale.ENGLISH)
	    	);

	    //settings.setAccessDeniedPage(AccessDeniedPage.class);
	    //settings.setPageExpiredErrorPage(PageExpiredErrorPage.class);
	    settings.setInternalErrorPage(InternalErrorPage.class);

	    // uploading stuff
	    EasyUploadStatusCommand uploadStatusResource = new EasyUploadStatusCommand();
	    //uploadStatusResource.registerAsSharedResource(this);
	    getSharedResources().add(uploadStatusResource.RESOURCE_NAME, uploadStatusResource);
	    EasyUploadCancelCommand uploadCancelResource = new EasyUploadCancelCommand();
	    //uploadCancelResource.registerAsSharedResource(this);
	    getSharedResources().add(uploadCancelResource.RESOURCE_NAME, uploadCancelResource);
    
	    // for the Combined Upload Status on the UploadFilesPage
	    CombinedUploadStatusCommand combinedUploadStatusResource = new CombinedUploadStatusCommand();
	    combinedUploadStatusResource.registerAsSharedResource(this);

	    // search pages are bookmarkable
	    mountBookmarkablePage("/search", PublicSearchResultPage.class);
	    mountBookmarkablePage("/advancedsearch", AdvSearchPage.class);

	    // other (main) pages 
	    mountBookmarkablePage("/organisations", OrganisationListPage.class);
	    mountBookmarkablePage("/about", AboutPage.class);
	    mountBookmarkablePage("/contact", ContactPage.class);
	    
	    mountBookmarkablePage("/termsofuse", TermsOfUsePage.class);
	    mountBookmarkablePage("/acknowledgements", AcknowledgementsPage.class);
	    
	    mountBookmarkablePage("/myprojects", MyProjectsSearchResultPage.class);
	    mountBookmarkablePage("/upload", UploadFilesPage.class);
	    mountBookmarkablePage("/members", MemberListPage.class);

	    // allows to view a project using id's like the SID, but should be PID in the future
	    //mountBookmarkablePage("/project", ProjectViewPage.class);
	     // RESTfull encoding is nicer
	    mount(new MixedParamUrlCodingStrategy("/project", ProjectViewPage.class, 
	    		new String[]{ProjectViewPage.SID_PARAM_KEY, ProjectViewPage.STREAMID_PARAM_KEY}));
	}

	// Required for UploadProgressBar (see javadoc there).
    @Override
    protected WebRequest newWebRequest(HttpServletRequest servletRequest) {
      return new EasyUploadWebRequest(servletRequest);
    }

    @Override
    protected void onDestroy() {
        // this makes sure that all data is rolledback before the application finishes
        //TODO: test it
    	EasyUploadProcesses.getInstance().cancelAllUploads();
        super.onDestroy();
    }

	@Override
	public final Session newSession(Request request, Response response) {
		return new DccdSession(request);
	}
}
