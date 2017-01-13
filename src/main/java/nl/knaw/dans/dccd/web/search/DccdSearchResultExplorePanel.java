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

import nl.knaw.dans.common.lang.search.Field;
import nl.knaw.dans.common.lang.search.SearchHit;
import nl.knaw.dans.common.lang.search.SearchRequest;
import nl.knaw.dans.common.lang.search.SearchResult;
import nl.knaw.dans.common.lang.search.simple.CombinedOptionalField;
import nl.knaw.dans.common.lang.search.simple.SimpleField;
import nl.knaw.dans.common.lang.search.simple.SimpleSearchRequest;
import nl.knaw.dans.common.wicket.WicketUtil;
import nl.knaw.dans.common.wicket.components.UnescapedLabel;
import nl.knaw.dans.common.wicket.components.pagebrowse.PageBrowseData;
import nl.knaw.dans.common.wicket.components.pagebrowse.PageBrowseLinkListener;
import nl.knaw.dans.common.wicket.components.pagebrowse.PageBrowsePanel;
import nl.knaw.dans.common.wicket.components.pagebrowse.PageBrowsePanel.PageBrowseLink;
import nl.knaw.dans.common.wicket.components.search.SearchBar;
import nl.knaw.dans.common.wicket.components.search.SearchPanel;
import nl.knaw.dans.common.wicket.components.search.criteria.CriteriumLabel;
import nl.knaw.dans.common.wicket.components.search.criteria.FilterCriterium;
import nl.knaw.dans.common.wicket.components.search.criteria.MultiFilterCriterium;
import nl.knaw.dans.common.wicket.components.search.criteria.SearchCriteriaPanel;
import nl.knaw.dans.common.wicket.components.search.criteria.TextSearchCriterium;
import nl.knaw.dans.common.wicket.components.search.facets.FacetConfig;
import nl.knaw.dans.common.wicket.components.search.facets.FacetPanel;
import nl.knaw.dans.common.wicket.components.search.model.SearchCriterium;
import nl.knaw.dans.common.wicket.components.search.model.SearchData;
import nl.knaw.dans.common.wicket.components.search.model.SearchModel;
import nl.knaw.dans.common.wicket.components.search.model.SearchRequestBuilder;
import nl.knaw.dans.common.wicket.components.search.results.SearchResultConfig;
import nl.knaw.dans.common.wicket.components.search.results.SearchSortPanel;
import nl.knaw.dans.common.wicket.exceptions.InternalWebError;
import nl.knaw.dans.dccd.common.lang.geo.LonLat;
import nl.knaw.dans.dccd.common.lang.geo.Marker;
import nl.knaw.dans.dccd.common.wicket.geo.GeoViewer;
import nl.knaw.dans.dccd.common.wicket.geo.LazyLoadingGeoViewer;
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
import nl.knaw.dans.dccd.web.search.years.YearRange;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.protocol.http.RequestUtils;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.validator.MinimumValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Note could not extend SearchResultPanel because of the geoViewer 
 *
 */
public abstract class DccdSearchResultExplorePanel extends SearchPanel
{

	private static final long serialVersionUID = -6771423045909049473L;
	private static final Logger	logger = LoggerFactory.getLogger(DccdSearchResultExplorePanel.class);

	
	// TEST
	// our 'model' data, could be in separate object
	private Integer fromYear = 1;
	public Integer getFromYear() {
		return fromYear;
	}
	public void setFromYear(Integer fromYear) {
		this.fromYear = fromYear;
	}
	//--------
	
	protected void onAdvancedSearchClicked(SearchModel searchModel)
	{
	}
	protected void onBrowseMoreClicked(SearchModel searchModel)
	{
	}
	
	public GeoViewer viewer;
	
	public DccdSearchResultExplorePanel(
    		final String wicketId
    		)
    {
		this(wicketId, new SearchModel());
    }

	public DccdSearchResultExplorePanel(
    		final String wicketId,
    		SearchModel searchModel
    		)
    {
		super(wicketId, searchModel);       
        init();
    }

	private void initRequestBuilder(SearchRequestBuilder requestBuilder)
	{
//		requestBuilder.setFacets(getConfig().getRefineFacets());
		
	}

    private void init()
	{
		initRequestBuilder(getRequestBuilder());
    	
        initComponents();
	}
    
	private void initComponents()
	{
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
		
		// result message (needs to come after page browse panel)
		add(new UnescapedLabel("resultMessage", getResultMessageModel()));

		initGeoViewer();
		initTimeline();
		
		// TEST
		Form form = new Form("form") {
			private static final long serialVersionUID = 1L;
			
			@Override
			protected void onSubmit() {
//				super.onSubmit();
				logger.debug("fromYear = " + Integer.toString(getFromYear()));
				
				// TEST
				// try to add or change the period query and search?

				final SimpleField<YearRange> deathYear = new SimpleField<YearRange>(DccdSB.TRIDAS_MEASUREMENTSERIES_INTERPRETATION_DEATHYEAR_NAME);
				deathYear.setValue(new YearRange(getFromYear(), 2014)); 
				final SimpleField<YearRange> lastYear = new SimpleField<YearRange>(DccdSB.TRIDAS_MEASUREMENTSERIES_INTERPRETATION_LASTYEAR_NAME);
				lastYear.setValue(new YearRange(getFromYear(), 2014)); 
				
				SearchModel searchModel = getSearchModel();// TEST
				List<SearchCriterium> criteria = new ArrayList<SearchCriterium>();
				searchModel.getRequestBuilder().setCriteria(criteria); //empty!!!!!
//				// we need to 'remove' it first, with the the name
//				List<SearchCriterium> criteria = searchModel.getRequestBuilder().getCriteria();
				
				// then add it
				
				/*
				getSearchModel().addCriterium(new FilterCriterium(lastYear, new AbstractReadOnlyModel<String>(){
					private static final long serialVersionUID = -7747467835417089340L;

					@Override
					public String getObject()
					{
						return "Period: " + Integer.toString(getFromYear()) + " - " + "2014";
					}
				}));
			    */

				/* */
				CombinedOptionalField<YearRange> endYearField = new CombinedOptionalField<YearRange>(new ArrayList<String>(){{
					add(DccdSB.TRIDAS_MEASUREMENTSERIES_INTERPRETATION_DEATHYEAR_NAME);
					add(DccdSB.TRIDAS_MEASUREMENTSERIES_INTERPRETATION_LASTYEAR_NAME);
				}});
				endYearField.setValue(new YearRange(getFromYear(), 2014));
				getSearchModel().addCriterium(new FilterCriterium(endYearField, new AbstractReadOnlyModel<String>(){
					private static final long serialVersionUID = 3313159546708159282L;

					@Override
					public String getObject()
					{
						return "Period: " + Integer.toString(getFromYear()) + " - " + "2014";
					}
				}));
				/* */				
				
				doSearch();
			}
		};
		add(form);
		// fromYear
		TextField fromYearValue = new TextField("fromYear", new PropertyModel(this, "fromYear"), Integer.class);
		form.add(fromYearValue);
		//fromYearValue.add(new MinimumValidator(1));
		

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
			            return new StringResourceModel(RI_RESULTMESSAGE_1, DccdSearchResultExplorePanel.this, null, new Object[] {
			                            queryString}).getObject();
			        }
			        else if (result.getTotalHits() > 1 && 
			        		result.getTotalHits() <= request.getLimit())
			        {
			            return new StringResourceModel(RI_RESULTMESSAGE_1PAGE, DccdSearchResultExplorePanel.this, null, new Object[] {
				                    result.getTotalHits(),
				                    queryString
			                    }).getObject();
			        }
			        else if (result.getTotalHits() > 1) 
			        {
			            return new StringResourceModel(RI_RESULTMESSAGE, DccdSearchResultExplorePanel.this, null, new Object[] {
				                    request.getOffset()+1,
				                    Math.min( request.getOffset() + request.getLimit(), result.getTotalHits()),
				                    result.getTotalHits(),
				                    queryString
			                    }).getObject();
			        }
			        else
			        {
			        	return new StringResourceModel(RI_NO_RESULTS, DccdSearchResultExplorePanel.this, null, new Object[] {
			                            queryString}).getObject();
			        }
		        }
		        else
		        {
		            if (result.getTotalHits() == 1)
		            {
		                return  new StringResourceModel(RI_RESULTMESSAGE_1_NIENTE, DccdSearchResultExplorePanel.this, null).getObject();
		            }
			        else if (result.getTotalHits() > 1)
			        {
		            	return new StringResourceModel(RI_RESULTMESSAGE_1PAGE_NIENTE, DccdSearchResultExplorePanel.this, null, new Object[] {
		        				result.getTotalHits()}).getObject();
		            }
		            else
		            {
		                return new StringResourceModel(RI_NO_RESULTS_NIENTE, DccdSearchResultExplorePanel.this, null).getObject();
		            }
		        }
			}
    	};
    }
	
	//--- DCCD specifics ---

	private void initGeoViewer()
	{
		// Add GeoViewer
		logger.debug("==> Adding GeoViewerPanel");
		viewer = new LazyLoadingGeoViewer("geoViewerPanel"){
			private static final long serialVersionUID = 8428537267101989479L;

			@Override
			protected List<Marker> produceMarkers() {
				//doSearch();
				return getMarkers();
			}		
		}; 
		add(viewer);
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
