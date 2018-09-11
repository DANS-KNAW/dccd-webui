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
package nl.knaw.dans.dccd.web.search;

import java.io.StringWriter;
import java.nio.charset.Charset;

import nl.knaw.dans.common.lang.search.SearchResult;
import nl.knaw.dans.common.lang.search.simple.SimpleSearchRequest;
import nl.knaw.dans.common.wicket.components.UnescapedLabel;
import nl.knaw.dans.common.wicket.components.search.SearchPanel;
import nl.knaw.dans.common.wicket.components.search.model.SearchData;
import nl.knaw.dans.common.wicket.components.search.model.SearchModel;
import nl.knaw.dans.common.wicket.components.search.model.SearchRequestBuilder;
import nl.knaw.dans.common.wicket.exceptions.InternalWebError;
import nl.knaw.dans.dccd.model.DccdUser;
import nl.knaw.dans.dccd.search.DccdSB;
import nl.knaw.dans.dccd.web.DccdSession;
import nl.knaw.dans.dccd.web.HomePage;
import nl.knaw.dans.dccd.web.base.BasePage;

import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebResource;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchResultDownloadPage extends BasePage
{
	private static final Logger	logger = LoggerFactory.getLogger(SearchResultDownloadPage.class);

	private static final String MIMETYPE_XML = "text/xml";
	private static final String FILE_EXTENSION_XML = "xml";

	private static final String MIMETYPE_CSV = "text/csv";
	private static final String FILE_EXTENSION_CSV = "csv";
	// the Unicode char represented by the UTF-8 byte order mark (EF BB BF).
	private static final String UTF8_BOM = "\uFEFF";

	private static final String NAME_RESULTS = "results";
	
	private DccdUser user;	
	private SearchPanel callingPanel;
	public transient CharSequence theData = null;
	private Page backPage;

	public SearchResultDownloadPage(SearchModel model, final DccdSearchResultPanel callingPanel) 
	{
		super(model);
		
		if(callingPanel == null) throw new IllegalArgumentException("calling panel should not be null");
		this.callingPanel = callingPanel;
		
		init();
	}
	
	private void init()
	{
	    // get the back page
    	backPage = ((DccdSession)Session.get()).getRedirectPage(getPage().getClass());
    	// The back button or link
		Link backButton = new Link("backButton", new ResourceModel("backButton"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick()
			{
		        // get the previous page, and try to browse back
		        Page page = backPage;
		        if (page != null)
		        {
		        //	if (page instanceof BasePage)
	        	//		((BasePage)page).refresh();
		        	setResponsePage(page);
		        }
		        else
		        {
		        	// just go back to a new instance of HomePage
		        	setResponsePage(HomePage.class);
		        }
			}
		};
		add(backButton);

        // result message 
		// Not the a panel for displaying just the total results message
		//add(new SearchResultMessagePanel("resultMessage", (SearchModel)getDefaultModel()));
		SearchData searchData = ((SearchModel)getDefaultModel()).getObject();
		SearchResult<? extends DccdSB> searchResult =  (SearchResult<? extends DccdSB>) searchData.getResult();
		SearchRequestBuilder originalRequestBuilder = searchData.getRequestBuilder();
		int totalHits = searchResult.getTotalHits();
		String queryString = originalRequestBuilder.getRequest().getQuery().getQueryString();
		add(new UnescapedLabel("resultMessage", 
				new StringResourceModel("search.download.resultMessage", this, null, 
						new Object[] {totalHits, Strings.escapeMarkup(queryString)})));
		
		// Need the user for the permission check
		user = (DccdUser) ((DccdSession) getSession()).getUser();

		// add form
		Form form = new Form("form");
		add(form);
		
		// Note mabe use a DynamicWebResource?
		WebResource export = new WebResource() 
		{
			private static final long	serialVersionUID	= -3015006109107869870L;

			@Override
			public IResourceStream getResourceStream() 
			{
				return getResourceStreamForXML();
			}

			@Override
			protected void setHeaders(WebResponse response) 
			{
				super.setHeaders(response);

				setHeadersForXML(response);
			}
		};
		export.setCacheable(false);

		final ResourceLink downloadLink = new ResourceLink("downloadSearchResults", export);
		downloadLink.setOutputMarkupPlaceholderTag(true);
		downloadLink.setVisible(false);
		downloadLink.setOutputMarkupId(true);
		form.add(downloadLink);
		
		final Label waitMsgLabel = new Label("myLabel", 
				new StringResourceModel("search.download.fileconstruction.waitMessage",this,null));
		waitMsgLabel.setOutputMarkupPlaceholderTag(true);
		waitMsgLabel.setVisible(true);
		waitMsgLabel.setOutputMarkupId(true);
		form.add(waitMsgLabel);
		
		IndicatingAjaxButton submitButton = new IndicatingAjaxButton("submitButton") {
			protected void onSubmit(AjaxRequestTarget target, Form<?> form)
			{
				// TODO Auto-generated method stub
				logger.debug("Onsubmit called, start work");

				theData = getAllResultsAsXMLInParts();//getAllResultsAsXML();
					
				logger.debug("Done with work");
				// TODO enable downlod button?
				downloadLink.setVisible(true);
				target.addComponent(downloadLink);
				
				waitMsgLabel.setVisible(false);
				target.addComponent(waitMsgLabel);
				
				this.setVisible(false);
				target.addComponent(this);
			}
	    };
	    submitButton.setOutputMarkupId(true);
	    form.add(submitButton);
	}
	
	//--- XML ---
	
	private IResourceStream getResourceStreamForXML() 
	{
		CharSequence txt = theData;//getAllResultsAsXML();
		
		StringResourceStream rs = new StringResourceStream(txt, MIMETYPE_XML);
		rs.setCharset(Charset.forName("UTF-8")); 
		return rs;
	}
	private void setHeadersForXML(WebResponse response)
	{
		// construct filename
		final String FILE_BASENAME = "dccdsearchresults";
		String filename = FILE_BASENAME;
		// NOTE maybe add a timestamp, but browsers/OS will offer a numbering to the user?
		filename = filename + "." + FILE_EXTENSION_XML;

		response.setAttachmentHeader(filename);
	}

	// Get all hits for the search request, which is originally paged for easy of use in the GUI
	private SearchResult<? extends DccdSB> getAllResults()
	{
		if (user == null)
		{
			// Not allowed, throw exception
			logger.debug("Not logged in while constructing SearchResults for Download");
			throw new InternalWebError();
		}

		SearchData searchData = ((SearchModel)getDefaultModel()).getObject();
		SearchResult<? extends DccdSB> searchResult =  (SearchResult<? extends DccdSB>) searchData.getResult();
		int limitForAll = searchResult.getTotalHits();
		
		return getResultsFor(0, limitForAll);
	}
	
	private SearchResult<? extends DccdSB> getResultsFor(final int offset, final int limit)
	{
		SearchData searchData = ((SearchModel)getDefaultModel()).getObject();
		SearchRequestBuilder originalRequestBuilder = searchData.getRequestBuilder();
		
		SearchRequestBuilder requestBuilder = new SearchRequestBuilder();
		requestBuilder.setLimit(limit);
		requestBuilder.setOffset(offset);
		// Use the same criteria
		// we are not changing the criteria so we can pass the list
		// Note that if criteria would change then we would need to implement a deep copy
		requestBuilder.setCriteria(originalRequestBuilder.getCriteria());
		// leave sorting and facets alone and construct the request
		SimpleSearchRequest request = requestBuilder.getRequest();
		
		// The request needs to be prepared for the search by adding filters dependent on the type of Page
		return (SearchResult<? extends DccdSB>) callingPanel.search(request);
	}

	// Produce the xml string
	// Note that it is done without an XML lib, but could use the DOM4J XMLWriter
	private String getAllResultsAsXMLInParts()
	{
		logger.debug("Start constructing result for download");
		java.io.StringWriter sw = new StringWriter();

		sw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"); // XML instruction
		sw.append("<" + NAME_RESULTS + ">");
		
		logger.debug("Constructing search results in XML for download");
		
		// do it in parts, a bit like paging
		int partSize = 200;
		SearchData searchData = ((SearchModel)getDefaultModel()).getObject();
		SearchResult<? extends DccdSB> searchResult =  (SearchResult<? extends DccdSB>) searchData.getResult();
		int totalNumberOfHits = searchResult.getTotalHits();
		int numberOfParts = (totalNumberOfHits + partSize -1)/partSize; // round up
		for (int partIndex = 0; partIndex < numberOfParts; partIndex++)
		{
			int offset = partIndex * partSize;
			logger.debug("Start part at: " + offset);
			SearchResult<? extends DccdSB> searchResults = getResultsFor(offset, partSize);
			sw.append(SearchResultDataConverter.getResultsAsXML(searchResults, user));
		}
		
		sw.append("</" + NAME_RESULTS + ">");
		
		logger.debug("End constructing result for download");

		return sw.toString();
	}

	//--- CSV, just for testing, but that has UTF-8 problems when importing into MS-Excel ---
	
	private IResourceStream getResourceStreamForCSV() 
	{
		// prepend BOM, some programs like it others don't 
		CharSequence txt = UTF8_BOM + getDataAsTabDelimitedText();
		
		StringResourceStream rs = new StringResourceStream(txt, MIMETYPE_CSV);
		rs.setCharset(Charset.forName("UTF-8")); 
		return rs;
	}
	private void setHeadersForCSV(WebResponse response)
	{
		// construct filename
		final String FILE_BASENAME = "dccdsearchresults";
		String filename = FILE_BASENAME;
		// NOTE maybe add a timestamp, but browsers/OS will offer a numbering to the user?
		filename = filename + "." + FILE_EXTENSION_CSV;

		response.setAttachmentHeader(filename);
	}
	
	// NOTE just a test!
	private String getDataAsTabDelimitedText()
	{
		java.io.StringWriter sw = new StringWriter();

		// TODO add a line for each search Hit
		for(int i=0; i < 10; i++)
		{
			sw.append("Just \t some \t test\n");
		}
		sw.append("Mehr Informationen \t über die DCCD Nutzung \t finden Sie hier.");
		
		return sw.toString();
	}

}
