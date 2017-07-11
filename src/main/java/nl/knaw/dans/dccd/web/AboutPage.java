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

import nl.knaw.dans.common.lang.user.User;
import nl.knaw.dans.dccd.web.authn.LoginPage;
import nl.knaw.dans.dccd.web.base.BasePage;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dev
 */
public class AboutPage extends BasePage
{
	private static final long serialVersionUID = 1L;
	public static final String PROJECT_CATEGORIES_PATH = "project.categories.path";
	public static final String TAXON_DATA_PATH = "taxon.data.path";
	private static Logger logger = LoggerFactory.getLogger(AboutPage.class);

	User currentUser;
	
	public AboutPage()
	{
		super();
		logger.error("About page called");
		currentUser = ((DccdSession) getSession()).getUser();

		
		Label loginForMore = new Label("loginForMore", getString("moreInfoWhenLoggedIn"));
		BookmarkablePageLink loginLink = new BookmarkablePageLink("login", LoginPage.class);
		add(loginForMore);
		add(loginLink);
		
		StatisticsDisplayPanel statsPanel = new StatisticsDisplayPanel("statsPanel");
		add(statsPanel);
		
		
		if(currentUser!=null)
		{
			loginForMore.setVisible(false);
			loginLink.setVisible(false);
		}
		

	}
	

		
}

