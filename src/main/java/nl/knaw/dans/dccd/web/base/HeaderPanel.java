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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import nl.knaw.dans.common.wicket.components.search.SearchBar;
import nl.knaw.dans.common.wicket.components.search.model.SearchModel;
import nl.knaw.dans.dccd.application.services.DccdConfigurationService;
import nl.knaw.dans.dccd.web.AboutPage;
import nl.knaw.dans.dccd.web.ContactPage;
import nl.knaw.dans.dccd.web.DccdSession;
import nl.knaw.dans.dccd.web.HomePage;
import nl.knaw.dans.dccd.web.authn.OrganisationListPage;
import nl.knaw.dans.dccd.web.personalbar.PersonalBarPanelDefault;
import nl.knaw.dans.dccd.web.personalbar.PersonalBarPanelLoggedIn;
import nl.knaw.dans.dccd.web.util.LocaleDropDown;

import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;

//import nl.knaw.dans.common.wicket.components.search.SimpleSearchPanel;

/** Header of each page
 *
 * @author paulboon
 */
public class HeaderPanel extends Panel {
	private static final long serialVersionUID = 6089148203178601703L;
    public static final String PERSONAL_BAR_PANEL = "personalBarPanel";
    
	public HeaderPanel(String id) {
		super(id);

		SearchBar searchBar = new SearchBar("searchPanel", nl.knaw.dans.dccd.web.search.pages.PublicSearchResultPage.class);
		add(searchBar);
		
		String maintenaceMessage = DccdConfigurationService.getService().getMaintenanceMessage();//"";
		Label maintenaceMessageLabel = new Label("maintenaceMessage", maintenaceMessage);
		maintenaceMessageLabel.setEscapeModelStrings(false); // allow html
		add(maintenaceMessageLabel);
		if (maintenaceMessage.isEmpty())maintenaceMessageLabel.setVisible(false);
		
//		add(new Label("header_msg", new ResourceModel("header_msg")));
		add(new BookmarkablePageLink("homeLink", HomePage.class));
		
		add(new BookmarkablePageLink("aboutLink", AboutPage.class));
		add(new BookmarkablePageLink("contactLink", ContactPage.class));
		add(new BookmarkablePageLink("organisationsLink", OrganisationListPage.class));
		
		BookmarkablePageLink advancedSearchLink = new BookmarkablePageLink("advancedSearchLink", 
				nl.knaw.dans.dccd.web.search.pages.AdvSearchPage.class);		
		add(advancedSearchLink);
		if ( ((DccdSession)Session.get()).getUser() != null)
			advancedSearchLink.setVisible(true);
		else
			advancedSearchLink.setVisible(false);

        Form form = new Form("form");
        add(form);

		List<Locale> locales = Arrays.asList(new Locale[] {
				Locale.ENGLISH,
				new Locale("nl"),
				Locale.GERMAN,
				Locale.FRENCH
        });
		LocaleDropDown localeDropDown = new LocaleDropDown("localeSelect", locales);
		form.add(localeDropDown);

        if ( ((DccdSession)Session.get()).getUser() != null)
            add(new PersonalBarPanelLoggedIn(PERSONAL_BAR_PANEL));
        else
            add(new PersonalBarPanelDefault(PERSONAL_BAR_PANEL));
	}
}
