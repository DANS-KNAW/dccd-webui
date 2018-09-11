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
import nl.knaw.dans.common.wicket.WicketUtil;
import nl.knaw.dans.common.wicket.components.UnescapedLabel;
import nl.knaw.dans.common.wicket.components.pagebrowse.PageBrowseData;
import nl.knaw.dans.common.wicket.components.pagebrowse.PageBrowseLinkListener;
import nl.knaw.dans.common.wicket.components.pagebrowse.PageBrowsePanel;
import nl.knaw.dans.common.wicket.components.pagebrowse.PageBrowsePanel.PageBrowseLink;
import nl.knaw.dans.common.wicket.components.search.SearchBar;
import nl.knaw.dans.common.wicket.components.search.SearchPanel;
import nl.knaw.dans.common.wicket.components.search.criteria.CriteriumLabel;
import nl.knaw.dans.common.wicket.components.search.criteria.SearchCriteriaPanel;
import nl.knaw.dans.common.wicket.components.search.criteria.TextSearchCriterium;
import nl.knaw.dans.common.wicket.components.search.facets.FacetConfig;
import nl.knaw.dans.common.wicket.components.search.facets.FacetPanel;
import nl.knaw.dans.common.wicket.components.search.model.SearchData;
import nl.knaw.dans.common.wicket.components.search.model.SearchModel;
import nl.knaw.dans.common.wicket.components.search.model.SearchRequestBuilder;
import nl.knaw.dans.common.wicket.components.search.results.SearchResultConfig;
import nl.knaw.dans.common.wicket.components.search.results.SearchSortPanel;
import nl.knaw.dans.common.wicket.exceptions.InternalWebError;
import nl.knaw.dans.dccd.common.lang.geo.LonLat;
import nl.knaw.dans.dccd.common.lang.geo.Marker;
import nl.knaw.dans.dccd.common.wicket.geo.GeoViewer;
import nl.knaw.dans.dccd.common.wicket.timeline.TimeMarker;
import nl.knaw.dans.dccd.common.wicket.timeline.Timeline;
import nl.knaw.dans.dccd.model.DccdUser;
import nl.knaw.dans.dccd.model.ProjectPermissionLevel;
import nl.knaw.dans.dccd.search.DccdObjectSB;
import nl.knaw.dans.dccd.search.DccdProjectSB;
import nl.knaw.dans.dccd.search.DccdSB;
import nl.knaw.dans.dccd.web.DccdSession;
import nl.knaw.dans.dccd.web.search.pages.LocationSearchResultPage;
import nl.knaw.dans.dccd.web.search.pages.PeriodSearchResultPage;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.protocol.http.RequestUtils;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Note could not extend SearchResultPanel because of the geoViewer and marker index
 *
 */
public abstract class DccdSearchResultPanel extends SearchPanel
{
	private static final long serialVersionUID = 8067439489635623029L;
	private static final Logger	logger = LoggerFactory.getLogger(DccdSearchResultPanel.class);

	protected void onAdvancedSearchClicked(SearchModel searchModel)
	{
	}
	protected void onBrowseMoreClicked(SearchModel searchModel)
	{
	}
    private static final String     PAGEBROWSE_PANEL   = "pageBrowsePanel";
	private SearchResultConfig config;	
	private PageBrowsePanel pageBrowsePanel;
	
	//DCCD 
	private boolean showTips;
	public GeoViewer viewer;
	public List<LonLat> locations = new ArrayList<LonLat>();// shared with the viewer via Model
	
	public DccdSearchResultPanel(
    		final String wicketId,
    		boolean showTips,
    		SearchResultConfig config)
    {
		this(wicketId, new SearchModel(), showTips, config);
    }

	public DccdSearchResultPanel(
    		final String wicketId,
    		SearchModel searchModel,
    		boolean showTips,
    		SearchResultConfig config)
    {
		super(wicketId, searchModel);
		this.config = config;
        this.showTips = showTips;
        
        init();
    }

	private void initRequestBuilder(SearchRequestBuilder requestBuilder)
	{
		requestBuilder.setFacets(getConfig().getRefineFacets());
		requestBuilder.setLimit(getConfig().getResultCount());
		getRequestBuilder().setSortFields(getConfig().getInitialSortFields());
	}

    private void init()
	{
		initRequestBuilder(getRequestBuilder());
    	
    	doSearch();
    	
        initComponents();
	}
    
	private void initComponents()
	{
		initSearchTips();
		initGeoViewer();
		initTimeline();
		
		// TODO refactor into parts

		// Search results download is only visible for logged in users
		SearchResultsDownloadPanel searchResultsDownloadPanel = 
			new SearchResultsDownloadPanel("downloadResultsPanel", getSearchModel(), this);
		searchResultsDownloadPanel.setVisible(isAllowedToDownloadSearchResults());
		add(searchResultsDownloadPanel);
				
		// criteria
        add(new SearchCriteriaPanel("searchCriteria", getSearchModel())
        {
			private static final long	serialVersionUID	= -6370349646809914607L;

			public boolean isVisible() 
        	{
        		return super.isVisible() && getRequestBuilder().getCriteria().size() > 1;
        	};
        }
        );

        // sort fields
        add(new SearchSortPanel("sortPanel", getSearchModel(), getConfig().getSortLinks()));

        // result message (needs to come after page browse panel)
        add(new UnescapedLabel("resultMessage", getResultMessageModel()));

    	// search hits
        add(new ListView("searchHits", new AbstractReadOnlyModel<List>()
    		{
				private static final long	serialVersionUID	= -8467661423061481825L;

				@Override
    			public List getObject()
    			{
					updateMarkerIndexes(); // The hitpanels need to know the marker index
					
    				return getSearchResult().getHits();
    			}
    		} 
	        )
	        {
	            private static final long serialVersionUID = -6597598635055541684L;
	
	            @Override
	            protected void populateItem(ListItem item)
	            {
	                final SearchHit<?> hit = (SearchHit<?>) item.getModelObject();
	
	                Panel hitPanel = getConfig().getHitPanelFactory().createHitPanel("searchHit", hit, getSearchModel());
	                if (hitPanel == null)
	                {
						logger.error("Could not create hit panel for searchHit "+ hit.toString() +". Programmer mistake.");
	                	throw new InternalWebError();
	                }
					
	                String oddOrEven = item.getIndex() % 2 == 0 ? "even" : "odd";
					hitPanel.add( new AttributeAppender("class", new Model(oddOrEven), " ") );
					item.add(hitPanel);
	            }
	            
	            @Override
	            public boolean isVisible()
	            {
	            	return getSearchResult().getHits().size() > 0;
	            }
	        }
        );
        

        // search refinement panel
		add(new SearchBar("refineSearchPanel")
			{
				private static final long	serialVersionUID	= -5980195347064339476L;

				@Override
				public void onSearch(String searchText)
				{
					DccdSearchResultPanel.this.getRequestBuilder().addCriterium( 
		        			new TextSearchCriterium(searchText, new Model<String>(
		        					CriteriumLabel.createFilterText(
		        							DccdSearchResultPanel.this.getString(SEARCHRESULTPANEL_CRITERIUMTEXT_REFINE_SEARCH), 
		        							searchText
		        						)))); 
				}

				@Override
				public boolean isVisible() 
				{
	            	return DccdSearchResultPanel.this.getSearchResult().getHits().size() > 1;
				}
			}
		);
		
		/**
		 * I had to make this enclosure by hand, because putting a wicket:enclosure in a
		 * wicket:enclosure caused a nasty bug when using the setResponsePage to render
		 * a page with this component on it. Everytime it would say that the "browseMore"
		 * component was forgotten in the markup. After almost 2 hours of searching it
		 * turned out to be a freaking bug in Wicket 1.4.7. 
		 */
		WebMarkupContainer refineFacets = new WebMarkupContainer("refineFacetsEnclosure")
			{
				private static final long	serialVersionUID	= 2474778991631709989L;

				public boolean isVisible() 
				{
					for (FacetConfig facetConfig : getConfig().getRefineFacets())
					{
						if (FacetPanel.isVisible(facetConfig, getSearchModel()))
							return true;
					}
					return false;
				};
			};
		add(refineFacets);

        // refinement facets
		refineFacets.add(new ListView<FacetConfig>("refineFacets", getConfig().getRefineFacets())
				{
					private static final long	serialVersionUID	= 7406250758535500272L;

					@Override
					protected void populateItem(ListItem<FacetConfig> item)
					{
						item.add(new FacetPanel("facet", getSearchModel(), item.getModelObject()));
					}				
				}
		);
		
        // browse more
		if (getConfig().showBrowseMore())
        {
			refineFacets.add(new Link("browseMore")
				{
					private static final long	serialVersionUID	= -6803231407654989149L;

					public void onClick() 
					{
						onBrowseMoreClicked(getSearchModel());
					}
				}
			);
        }
        else
        { 
        	WicketUtil.hide(refineFacets, "browseMore");
        }

		// page browse panel
		PageBrowseData pbData = new PageBrowseData(
				getRequestBuilder().getOffset()+1, 
				getRequestBuilder().getLimit(), 
    			getSearchResult().getTotalHits()
    		);
		pageBrowsePanel = new PageBrowsePanel(
        		PAGEBROWSE_PANEL, 
        		new Model<PageBrowseData>(pbData)
        		{
					private static final long	serialVersionUID	= 1943406023315332637L;

					@Override
        			public PageBrowseData getObject()
        			{
        				PageBrowseData pbData = super.getObject();
        				pbData.init(
        						getRequestBuilder().getOffset()+1, 
        						getRequestBuilder().getLimit(), 
        						getSearchResult().getTotalHits()
        					);
        				return pbData;
        			}
        		}, 
        		new PageBrowseLinkListener()
		        {
		            private static final long serialVersionUID = 5814085953388070471L;
		            public void onClick(PageBrowseLink plink)
		            {
						getRequestBuilder().setOffset(plink.getTargetItemStart()-1);
		            }
		        }
        );
        add(pageBrowsePanel);
       
        // Also put a result navigation panel at the top of the result list and not only at the bottom
        PageBrowsePanel pageBrowsePanelTop = new PageBrowsePanel(
				"pageBrowsePanelTop", 
        		new Model<PageBrowseData>(pbData)
        		{
					private static final long	serialVersionUID	= 285669742367238762L;

					@Override
        			public PageBrowseData getObject()
        			{
        				PageBrowseData pbData = super.getObject();
        				pbData.init(
        						getRequestBuilder().getOffset()+1, 
        						getRequestBuilder().getLimit(), 
        						getSearchResult().getTotalHits()
        					);
        				return pbData;
        			}
        		}, 
        		new PageBrowseLinkListener()
		        {
					private static final long	serialVersionUID	= -5680633860752419691L;

					public void onClick(PageBrowseLink plink)
		            {
						getRequestBuilder().setOffset(plink.getTargetItemStart()-1);
		            }
		        }
        );
        add(pageBrowsePanelTop);
        
		// select the number of results per page, only at the top of the results
		ResultsPerPageSelectionPanel resultsPerPageSelectionPanel = new ResultsPerPageSelectionPanel("resultsPerPageSelectionPanel", 
				new Model<SearchRequestBuilder>() {
					private static final long serialVersionUID = 1L;
			
			    // pull the request builder, because we need to set/get the 'limit'
				@Override
				public SearchRequestBuilder getObject() {
					return getRequestBuilder();
				}
		}) {
			private static final long serialVersionUID = 1L;
		
			@Override
			public boolean isVisible() {
				// hide when there are no results
				return (getSearchResult().getTotalHits() > 0);
			}
		};
		add(resultsPerPageSelectionPanel);


        // advanced search
        if (getConfig().showAdvancedSearch())
        {
        	add(new Link("advancedSearch")
	        	{
					private static final long	serialVersionUID	= -1905413983732583324L;

					@Override
	        		public void onClick()
	        		{
						onAdvancedSearchClicked(getSearchModel());
	        		}
	        	}
	    	);
        }
        else
        {
        	hide("advancedSearch");
        }
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
			            return new StringResourceModel(RI_RESULTMESSAGE_1, DccdSearchResultPanel.this, null, new Object[] {
			            		Strings.escapeMarkup(queryString)}).getObject();
			        }
			        else if (result.getTotalHits() > 1 && 
			        		result.getTotalHits() <= request.getLimit())
			        {
			            return new StringResourceModel(RI_RESULTMESSAGE_1PAGE, DccdSearchResultPanel.this, null, new Object[] {
				                    result.getTotalHits(),
				                    Strings.escapeMarkup(queryString)
			                    }).getObject();
			        }
			        else if (result.getTotalHits() > 1) 
			        {
			            return new StringResourceModel(RI_RESULTMESSAGE, DccdSearchResultPanel.this, null, new Object[] {
				                    request.getOffset()+1,
				                    Math.min( request.getOffset() + request.getLimit(), result.getTotalHits()),
				                    result.getTotalHits(),
				                    Strings.escapeMarkup(queryString)
			                    }).getObject();
			        }
			        else
			        {
			        	return new StringResourceModel(RI_NO_RESULTS, DccdSearchResultPanel.this, null, new Object[] {
			        			Strings.escapeMarkup(queryString)}).getObject();
			        }
		        }
		        else
		        {
		            if (result.getTotalHits() == 1)
		            {
		                return  new StringResourceModel(RI_RESULTMESSAGE_1_NIENTE, DccdSearchResultPanel.this, null).getObject();
		            }
			        else if (result.getTotalHits() > 1 && 
			        		pageBrowsePanel.getCurrentPage() == pageBrowsePanel.getLastPage())
			        {
		            	return new StringResourceModel(RI_RESULTMESSAGE_1PAGE_NIENTE, DccdSearchResultPanel.this, null, new Object[] {
		        				result.getTotalHits()}).getObject();
			        }
		            else if (result.getTotalHits() > 1)
		            {
		            	return new StringResourceModel(RI_RESULTMESSAGE_NIENTE, DccdSearchResultPanel.this, null, new Object[] {
		            				request.getOffset()+1,
		            				Math.min( request.getOffset() + request.getLimit(), result.getTotalHits()),
		            				result.getTotalHits()}).getObject();
		            }
		            else
		            {
		                return new StringResourceModel(RI_NO_RESULTS_NIENTE, DccdSearchResultPanel.this, null).getObject();
		            }
		        }
			}
    	};
    }

	public SearchResultConfig getConfig()
	{
		return config;
	}
	
	public void setConfig(SearchResultConfig config)
	{
		this.config = config;
	}
	
	//--- DCCD specifics ---
	
    private boolean isAllowedToDownloadSearchResults()
    {
    	boolean result = false;
    	
    	if (((DccdSession)Session.get()).isLoggedIn())
    	{
    		// TEST 
    		// only allow admin and some test users
			// Need the user for the permission check
			//DccdUser userLogedIn = (DccdUser) ((DccdSession) getSession()).getUser();
		    //if (userLogedIn != null &&
			//    	(userLogedIn.getId().equals("rowinvanlanen") || // Test user
			//    	userLogedIn.getId().equals("jansm103") || // Test user
			//    	userLogedIn.getId().equals("paulboon") || // Test user
			//    	userLogedIn.hasRole(DccdUser.Role.ADMIN)))
		    //{
		    //	result = true;
		    //}
    		result = true;
    	}
    	
    	return result;
    }

	private void initSearchTips()
	{
		if (showTips)
		{
			WebMarkupContainer searchTips = new WebMarkupContainer("searchTips")
			{
				private static final long	serialVersionUID	= 1234523335L;
				
				@Override
				public boolean isVisible()
				{
					return !StringUtils.isBlank(
							getRequestBuilder().getRequest().getQuery().getQueryString()
						);
				}
			};
			WebMarkupContainer noResultsTip = new WebMarkupContainer("noResultsTip")
			{
				private static final long	serialVersionUID	= 12345235L;

				public boolean isVisible() 
				{
					return getSearchResult().getTotalHits() == 0;
				};
			};
			searchTips.add(noResultsTip);
			
			add(searchTips);
		}
		else
		{
			hide("searchTips");
		}		
	}
	
	private void initGeoViewer()
	{
		// Add GeoViewer
		logger.debug("Adding GeoViewerPanel");
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
		
		// Link to page with locations of all results on a map
		add(new Link("fullMap") {
			private static final long	serialVersionUID	= 1L;
			@Override
			public void onClick()
			{
				// support back navigation
				((DccdSession)Session.get()).setRedirectPage(LocationSearchResultPage.class, getPage());

				setResponsePage(new LocationSearchResultPage(getSearchModel(), DccdSearchResultPanel.this));
			}	
		});
		
		
		add(new Link("allPeriods") {
			private static final long	serialVersionUID	= 1L;
			@Override
			public void onClick()
			{
				// support back navigation
				((DccdSession)Session.get()).setRedirectPage(PeriodSearchResultPage.class, getPage());
				
				setResponsePage(new PeriodSearchResultPage(getSearchModel(), DccdSearchResultPanel.this));
				
				// TODO use combined with setResponsePage(new SearchResultExplorePage(getSearchModel()));
			}	
		});
	}
	
	private void initTimeline()
	{
		logger.debug("Adding TimelinePanel");
		
		Timeline timeline = new Timeline("timelinePanel", new ListModel<TimeMarker>() 
		{
			private static final long serialVersionUID = 2013486801867455078L;

			@Override
			public List<TimeMarker> getObject()
			{
				DccdUser user = (DccdUser) ((DccdSession) getSession()).getUser();
				
				// produce markers on page creation
				List<TimeMarker> m = new ArrayList<TimeMarker>();
				// add one for TESTING
				//m.add(new TimeMarker(1666, 1966, "Test Marker"));
				
				SearchData searchData = getSearchModel().getObject();
				SearchResult<? extends DccdSB> searchResult =  (SearchResult<? extends DccdSB>) searchData.getResult();

				// Note that we don't get all hits, because the actual hits returned are paged
				for(SearchHit<? extends DccdSB> hit : searchResult.getHits())
				{
					DccdSB dccdSB = hit.getData();
					
					TimeMarker marker = getTimeMarker(dccdSB, user);
					if(marker != null) m.add(marker);
				}
				
				return m;
			}
		});
		
		add(timeline);
	}
	
	/**
	 * Determine time period for this hit = [min, max]
	 * and create a time marker
	 * 
	 * @param dccdSB
	 * @return the marker, or null if there was no year information in the SB
	 */
	private TimeMarker getTimeMarker(DccdSB dccdSB, DccdUser user)
	{
		TimeMarker marker = null;

		// check that we are allowed to see measurementSeries level for this!
		//????
		ProjectPermissionLevel effectivelevel = dccdSB.getEffectivePermissionLevel(user);
		Boolean isAllowedToViewTimeRange =  ProjectPermissionLevel.SERIES.isPermittedBy(effectivelevel);

		if (isAllowedToViewTimeRange)
		{
			// concat all the lists, but only non null elements
			List<Integer> years = new ArrayList<Integer>();
			List<Integer> yearsFromTridas = dccdSB.getTridasMeasurementseriesInterpretationPithyear();
			if (yearsFromTridas != null) 
			{
				for(Integer year : yearsFromTridas)
				{
					if (year != null) years.add(year);
				}
			}
			yearsFromTridas = dccdSB.getTridasMeasurementseriesInterpretationFirstyear();
			if (yearsFromTridas != null) 
			{
				for(Integer year : yearsFromTridas)
				{
					if (year != null) years.add(year);
				}
			}
			yearsFromTridas = dccdSB.getTridasMeasurementseriesInterpretationLastyear();
			if (yearsFromTridas != null) 
			{
				for(Integer year : yearsFromTridas)
				{
					if (year != null) years.add(year);
				}
			}
			yearsFromTridas = dccdSB.getTridasMeasurementseriesInterpretationDeathyear();
			if (yearsFromTridas != null) 
			{
				for(Integer year : yearsFromTridas)
				{
					if (year != null) years.add(year);
				}
			}
			
			if (!years.isEmpty())
			{
				// we have at least one year (and it is not null)
				Integer min = years.get(0);
				Integer max = min;
				
				for(int i=1; i < years.size(); i++)
				{
					if (years.get(i) < min) min = years.get(i);
					if (years.get(i) > max) max = years.get(i);
				}
				// create the marker
				// the same info is used as the Map is using, making it easy to see corresponding markers
				marker = new TimeMarker(min, max, getMarkerInfo(dccdSB));//dccdSB.getTridasProjectTitle());
			}
		}
		
		return marker;
	}
	
	// The hitpanels need to know the index of the markers on the (geo)map
	// This markerIndex is used to visually connect the hits on the list with the map
	private void updateMarkerIndexes()
	{
		DccdUser user = (DccdUser) ((DccdSession) getSession()).getUser();
		
		SearchData searchData = getSearchModel().getObject();
		SearchResult<? extends DccdSB> searchResult =  (SearchResult<? extends DccdSB>) searchData.getResult();

		int indexCounter = 0;
		
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
				logger.debug("Has Geo location");
				if (isAllowedToViewLocation)
				{
					logger.debug("Allowed to view location");

					logger.debug("Setting Marker index to: " + indexCounter);
					dccdSB.latLngMarkerIndex = indexCounter;
					indexCounter++;
				}
				else
				{
					logger.debug("NOT allowed to view location");
				}
			}
		}
	}
	
	/**
	 * Construct the html formatted info 
	 * that the Marker pop-up will display when clicked on the map
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
