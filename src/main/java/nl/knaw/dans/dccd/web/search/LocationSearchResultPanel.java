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

import java.util.ArrayList;
import java.util.List;

import nl.knaw.dans.common.lang.search.SearchHit;
import nl.knaw.dans.common.lang.search.SearchRequest;
import nl.knaw.dans.common.lang.search.SearchResult;
import nl.knaw.dans.common.wicket.components.UnescapedLabel;
import nl.knaw.dans.common.wicket.components.search.SearchPanel;
import nl.knaw.dans.common.wicket.components.search.model.SearchData;
import nl.knaw.dans.common.wicket.components.search.model.SearchModel;
import nl.knaw.dans.common.wicket.components.search.model.SearchRequestBuilder;
import nl.knaw.dans.dccd.common.lang.geo.Marker;
import nl.knaw.dans.dccd.common.wicket.geo.GeoViewer;
import nl.knaw.dans.dccd.common.wicket.geo.LazyLoadingGeoViewer;
import nl.knaw.dans.dccd.model.DccdUser;
import nl.knaw.dans.dccd.model.ProjectPermissionLevel;
import nl.knaw.dans.dccd.search.DccdObjectSB;
import nl.knaw.dans.dccd.search.DccdProjectSB;
import nl.knaw.dans.dccd.search.DccdSB;
import nl.knaw.dans.dccd.web.DccdSession;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.RequestUtils;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Display a (large) map with locations from all search results (unpaged)
 * 
 * Note could not extend SearchResultPanel because of the geoViewer and marker index
 *
 */
public abstract class LocationSearchResultPanel extends SearchPanel
{
	private static final long	serialVersionUID	= -7771120102898231306L;
	private static final Logger	logger = LoggerFactory.getLogger(LocationSearchResultPanel.class);

	public GeoViewer viewer;
//	public List<LonLat> locations = new ArrayList<LonLat>();// shared with the viewer via Model
	
	public LocationSearchResultPanel(final String wicketId)
    {
		this(wicketId, new SearchModel());
    }

	public LocationSearchResultPanel(
    		final String wicketId,
    		SearchModel searchModel
    		)
    {
		super(wicketId, searchModel);
        
        init();
    }

	private void initRequestBuilder(SearchRequestBuilder requestBuilder)
	{
		// Force to max. number of results, but not sure what happens when a lot is found!
		// There must be a limit, so calculate the max.
		final SearchResult<?> result 	= getSearchResult();
		int limitForAll = result.getTotalHits();
		
		// Note 
		// if page load is not working because the hits consume to much memory, 
		// maybe the hits could be retrieved in parts (using offset+limit) and then the locations added
		// But I don't know how to implement it!
		
		requestBuilder.setLimit(limitForAll);
		requestBuilder.setOffset(0);
	}

    private void init()
	{    	
    	
		initRequestBuilder(getRequestBuilder());

//		doSearch();
    	
        initComponents();
	}

	private void initComponents()
	{
		initGeoViewer();

        // result message 
        add(new UnescapedLabel("resultMessage", getResultMessageModel()));
	}

    public IModel<String> getResultMessageModel()
    {
    	return new AbstractReadOnlyModel<String>()
    	{
			private static final long	serialVersionUID	= -3354392109873495635L;

			@Override
			public String getObject()
			{
		    	final SearchRequest request		= getSearchRequest();
		        final SearchResult<?> result 	= getSearchResult();
		
		        String queryString = request.getQuery().getQueryString();
		        if (!StringUtils.isBlank(queryString))
		        {
					if (result.getTotalHits() == 1)
			        {
			            return new StringResourceModel(RI_RESULTMESSAGE_1, LocationSearchResultPanel.this, null, new Object[] {
			            		Strings.escapeMarkup(queryString)}).getObject();
			        }
			        else if (result.getTotalHits() > 1 && 
			        		result.getTotalHits() <= request.getLimit())
			        {
			            return new StringResourceModel(RI_RESULTMESSAGE_1PAGE, LocationSearchResultPanel.this, null, new Object[] {
				                    result.getTotalHits(),
				                    Strings.escapeMarkup(queryString)
			                    }).getObject();
			        }
			        else if (result.getTotalHits() > 1) 
			        {
			            return new StringResourceModel(RI_RESULTMESSAGE, LocationSearchResultPanel.this, null, new Object[] {
				                    request.getOffset()+1,
				                    Math.min( request.getOffset() + request.getLimit(), result.getTotalHits()),
				                    result.getTotalHits(),
				                    Strings.escapeMarkup(queryString)
			                    }).getObject();
			        }
			        else
			        {
			        	return new StringResourceModel(RI_NO_RESULTS, LocationSearchResultPanel.this, null, new Object[] {
			        			Strings.escapeMarkup(queryString)}).getObject();
			        }
		        }
		        else
		        {
		            if (result.getTotalHits() == 1)
		            {
		            	// One hit
		                return  new StringResourceModel(RI_RESULTMESSAGE_1_NIENTE, LocationSearchResultPanel.this, null).getObject();
		            }
		            else if(result.getTotalHits() <= request.getLimit()) 
		            {
		            	// All hits are shown
		            	return new StringResourceModel(RI_RESULTMESSAGE_1PAGE_NIENTE, LocationSearchResultPanel.this, null, new Object[] {
        				result.getTotalHits()}).getObject();
		            	
		            }
		            else if (result.getTotalHits() > 1)
		            {
		            	// Not all hits are shown
		            	return new StringResourceModel(RI_RESULTMESSAGE_NIENTE, LocationSearchResultPanel.this, null, new Object[] {
		            				request.getOffset()+1,
		            				Math.min( request.getOffset() + request.getLimit(), result.getTotalHits()),
		            				result.getTotalHits()}).getObject();
		            }
		            else
		            {
		                return new StringResourceModel(RI_NO_RESULTS_NIENTE, LocationSearchResultPanel.this, null).getObject();
		            }
		        }
			}
    	};
    }

	//--- GeoViewer related ---	

	private void initGeoViewer()
	{
		// Add GeoViewer
		logger.debug("==> Adding GeoViewerPanel");
		viewer = new LazyLoadingGeoViewer("geoViewerPanel"){
			private static final long serialVersionUID = 8428537267101989479L;

			@Override
			protected List<Marker> produceMarkers() {
				doSearch();
				return getMarkers();
			}		
		}; 
		add(viewer);
		
		/*
		// ehhh, a trick that works, respond is called after OnDomReady..
		// the AjaxLazyLoadPanel is using something like this
		viewer.add( new AbstractDefaultAjaxBehavior() {
			private static final long serialVersionUID = -7720550178388585424L;

			@Override
	        protected void respond(AjaxRequestTarget target) {
				doSearch();
	            // your code here
				//viewer.clearMarkers();
				viewer.addMarkers(getMarkers(), target);
	        }

	        @Override
	        public void renderHead(IHeaderResponse response) {
	            super.renderHead( response );
	            response.renderOnDomReadyJavascript( getCallbackScript().toString() );
	        }
	    });	
	    */
	}
    
	/*
	// Refactoring Note: 
	// following code is almost 'identical' to that in the DccdSearchResultPanel
	
	private void initGeoViewer()
	{
		// Add GeoViewer
		logger.debug("==> Adding GeoViewerPanel");
		GeoViewer viewer = new GeoViewer("geoViewerPanel", new ListModel<Marker>() 
		{
			private static final long	serialVersionUID	= -8596739773617282567L;

			@Override
			public List<Marker> getObject()
			{
				// Use Search Hits from the current model object to construct the list of locations
				// Need the user for the permission check
				DccdUser user = (DccdUser) ((DccdSession) getSession()).getUser();
				
				List<Marker> loc = new ArrayList<Marker>();
				//SearchResult<? extends DccdSB> searchResult =  (SearchResult<? extends DccdSB>) getSearchResult();
				SearchData searchData = getSearchModel().getObject();
				SearchResult<? extends DccdSB> searchResult =  (SearchResult<? extends DccdSB>) searchData.getResult();

				// Note that we don't get all hits, because the actual hits returned are paged
				for(SearchHit<? extends DccdSB> hit : searchResult.getHits())
				{
					DccdSB dccdSB = hit.getData();
					
					// Determine if it is permitted to show it
					logger.debug("Hit: " + dccdSB.getId() + " level: " + dccdSB.getPermissionDefaultLevel());
					// The location is from the ObjectEntity 'Level'
					// allow only if admin, or owner, or level is "object" or better...
					ProjectPermissionLevel effectivelevel = dccdSB.getEffectivePermissionLevel(user);
					Boolean isAllowedToViewLocation =  ProjectPermissionLevel.OBJECT.isPermittedBy(effectivelevel);
					
					// Note: if we want to have a logged-in user we would need to check that as well:
					// if (user == null) isAllowedToViewLocation = false;
					// But then the 'Public' search would always have an empty map!
					
					//logger.debug("===> hit: " + dccdSB.getId());
					// Get the Lat and Lng and put it on the map
					if (dccdSB.hasLatLng())
					{
						logger.debug("Geo location: (" + dccdSB.getLng() + "," + dccdSB.getLat() + ")");
						if (isAllowedToViewLocation)
						{
							logger.debug("Allowed to view location");
							loc.add(new Marker(dccdSB.getLng(), dccdSB.getLat(), getMarkerInfo(dccdSB)));
						}
						else
						{
							logger.debug("NOT allowed to view location");
						}
					}
				}				
				return loc;
			}
		});

		add(viewer);
	}
	*/
	
	/**
	 * Get all markers corresponding to the search results (hits)
	 * 
	 * @return
	 */
	public List<Marker> getMarkers()
	{
		// Use Search Hits from the current model object to construct the list of locations
		// Need the user for the permission check
		DccdUser user = (DccdUser) ((DccdSession) getSession()).getUser();
		
		List<Marker> loc = new ArrayList<Marker>();
		//SearchResult<? extends DccdSB> searchResult =  (SearchResult<? extends DccdSB>) getSearchResult();
		SearchData searchData = getSearchModel().getObject();
		SearchResult<? extends DccdSB> searchResult =  (SearchResult<? extends DccdSB>) searchData.getResult();

		// Note that we don't get all hits, because the actual hits returned are paged
		for(SearchHit<? extends DccdSB> hit : searchResult.getHits())
		{
			DccdSB dccdSB = hit.getData();
				
			// Determine if it is permitted to show it
			logger.debug("Hit: " + dccdSB.getId() + " level: " + dccdSB.getPermissionDefaultLevel());
			// The location is from the ObjectEntity 'Level'
			// allow only if admin, or owner, or level is "object" or better...
			ProjectPermissionLevel effectivelevel = dccdSB.getEffectivePermissionLevel(user);
			Boolean isAllowedToViewLocation =  ProjectPermissionLevel.OBJECT.isPermittedBy(effectivelevel);
			
			// Note: if we want to have a logged-in user we would need to check that as well:
			// if (user == null) isAllowedToViewLocation = false;
			// But then the 'Public' search would always have an empty map!
			
			//logger.debug("===> hit: " + dccdSB.getId());
			// Get the Lat and Lng and put it on the map
			if (dccdSB.hasLatLng())
			{
				logger.debug("Geo location: (" + dccdSB.getLng() + "," + dccdSB.getLat() + ")");
				if (isAllowedToViewLocation)
				{
					logger.debug("Allowed to view location");
					loc.add(new Marker(dccdSB.getLng(), dccdSB.getLat(), getMarkerInfo(dccdSB)));
				}
				else
				{
					logger.debug("NOT allowed to view location");
				}
			}
		}				
		return loc;
	}
	
	/**
	 * Note that this code is identical to the DccdSearchResultPanel.getMarkerInfo method
	 * But that might change...
	 * 
	 * @param dccdHit
	 * @return
	 */
	private String getMarkerInfo(DccdSB dccdHit)
	{
		String titleStr = "";
		String identifierStr = "";
		
		// construct title
		if (dccdHit instanceof DccdProjectSB)
		{
			// Project
			titleStr = dccdHit.getTridasProjectTitle();
		}
		else
		{
			// Object
			if (dccdHit.hasTridasObjectTitle())
				titleStr = dccdHit.getTridasObjectTitle().get(0);
		}

		// Titles can have newlines, replace them
		titleStr = titleStr.replaceAll("[\\r\\n]", " ");
		// escape the string for html
		titleStr = Strings.escapeMarkup(titleStr, true, true).toString();

		// NOTE we always show the ProjectId because the ObjectId is incorrectly indexed 
		// But also the project ID is forced to be unique for archived/published projects
		// 
		if (dccdHit.hasTridasProjectIdentifier())
			identifierStr = dccdHit.getTridasProjectIdentifier();

		// Add domain
		String domainStr = "";
		if (dccdHit.hasTridasProjectIdentifierDomain()) 
		{
			domainStr = dccdHit.getTridasProjectIdentifierDomain();
			identifierStr = identifierStr + " ("+ domainStr + ")";
		}

		// escape the string for html
		identifierStr = Strings.escapeMarkup(identifierStr, true, true).toString();
		
		// return titleStr + "</br>" + identifierStr;
		String absPath = RequestUtils.toAbsolutePath(RequestCycle.get()
				.getRequest().getRelativePathPrefixToWicketHandler());
		
		if (!absPath.endsWith("/"))
			absPath = absPath + "/";
		
		
		// Link for the Project/object, could use a URLBuilder or something smarter
		String hrefStr = absPath + "project/"+ dccdHit.getPid();

		if (dccdHit instanceof DccdObjectSB)
		{
			// append id for the object entity
			hrefStr += "/" + dccdHit.getDatastreamId();
		}
		
		//return titleStr + "</br>" + "<a href='" + hrefStr + "'>" + identifierStr + "</a>";
		// put the link on the title, we could then drop the identifier if we want to
		return "<a href='" + hrefStr + "'>" + titleStr + "</a>" + "</br>" +  identifierStr;
	}
}
