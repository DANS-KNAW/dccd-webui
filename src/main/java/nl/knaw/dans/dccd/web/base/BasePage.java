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
package nl.knaw.dans.dccd.web.base;


import nl.knaw.dans.dccd.web.DccdSession;
import nl.knaw.dans.dccd.web.authn.LoginPage;
import nl.knaw.dans.dccd.web.project.ProjectViewPage;

import org.apache.log4j.Logger;
import org.apache.wicket.IPageMap;
import org.apache.wicket.PageParameters;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/** The site's webpages extend this
 *
 * @author paulboon
 */
public abstract class BasePage extends WebPage
{	
	private static Logger logger = Logger.getLogger(BasePage.class);
	
    /**
     * Constructor
     */
    public BasePage() {
    	super();
        init();
    }

    /**
     * Construct.
     * @param model
     */
    public BasePage(IModel model) {
        super(model);
        init();
    }

    public BasePage(PageParameters params) {
    	super(params);
        init();
    }

	public BasePage(IPageMap pageMap, PageParameters params) {
    	super(pageMap, params);
        init();
	}

	private void init()
	{
        add(new Label("title", new PropertyModel(this, "title")));

        add(new HeaderPanel("header"));
        add(new FooterPanel("footer"));
	}

	//public abstract String getTitle();
    public String getTitle() { return "";}

    /**
     * Refresh the contents of the page. Subclasses may override.
     */
    public void refresh()
    {
        //logger.warn("Refresh called on " + this.getClass().getName() + " while it is not implementing refresh!");
    }
    
	protected void redirectIfNotLoggedIn()
	{
		logger.debug("Check if we need to Redirecting to login page");

		if (!isLoggedIn())
		{
			logger.debug("User not logged in; Redirecting to login page");

			// redirect to login page and enable the login to return here
			throw new RestartResponseAtInterceptPageException(LoginPage.class);
		}
	}

	private boolean isLoggedIn()
	{
		return ((DccdSession) Session.get()).isLoggedIn();
	}

}
