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

import nl.knaw.dans.common.lang.search.SearchRequest;
import nl.knaw.dans.common.lang.search.SearchResult;
import nl.knaw.dans.common.wicket.components.UnescapedLabel;
import nl.knaw.dans.common.wicket.components.search.BaseSearchPanel;
import nl.knaw.dans.common.wicket.components.search.model.SearchModel;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;

public class SearchResultMessagePanel extends BaseSearchPanel
{
	private static final long	serialVersionUID	= 2516271309227287593L;

	public SearchResultMessagePanel(String wicketId, SearchModel searchModel)
	{
		super(wicketId, searchModel);

		init();
	}
	
	private void init()
	{
        // result message 
        add(new UnescapedLabel("resultMessageTxt", getResultMessageModel()));
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
			            return new StringResourceModel(RI_RESULTMESSAGE_1, SearchResultMessagePanel.this, null, new Object[] {
			            		Strings.escapeMarkup(queryString)}).getObject();
			        }
			        else if (result.getTotalHits() > 1 && 
			        		result.getTotalHits() <= request.getLimit())
			        {
			            return new StringResourceModel(RI_RESULTMESSAGE_1PAGE, SearchResultMessagePanel.this, null, new Object[] {
				                    result.getTotalHits(),
				                    Strings.escapeMarkup(queryString)
			                    }).getObject();
			        }
			        else if (result.getTotalHits() > 1) 
			        {
			            return new StringResourceModel(RI_RESULTMESSAGE, SearchResultMessagePanel.this, null, new Object[] {
				                    request.getOffset()+1,
				                    Math.min( request.getOffset() + request.getLimit(), result.getTotalHits()),
				                    result.getTotalHits(),
				                    Strings.escapeMarkup(queryString)
			                    }).getObject();
			        }
			        else
			        {
			        	return new StringResourceModel(RI_NO_RESULTS, SearchResultMessagePanel.this, null, new Object[] {
			        			Strings.escapeMarkup(queryString)}).getObject();
			        }
		        }
		        else
		        {
		            if (result.getTotalHits() == 1)
		            {
		            	// One hit
		                return  new StringResourceModel(RI_RESULTMESSAGE_1_NIENTE, SearchResultMessagePanel.this, null).getObject();
		            }
		            else if(result.getTotalHits() <= request.getLimit()) 
		            {
		            	// All hits are shown
		            	return new StringResourceModel(RI_RESULTMESSAGE_1PAGE_NIENTE, SearchResultMessagePanel.this, null, new Object[] {
        				result.getTotalHits()}).getObject();
		            	
		            }
		            else if (result.getTotalHits() > 1)
		            {
		            	// Not all hits are shown
		            	return new StringResourceModel(RI_RESULTMESSAGE_NIENTE, SearchResultMessagePanel.this, null, new Object[] {
		            				request.getOffset()+1,
		            				Math.min( request.getOffset() + request.getLimit(), result.getTotalHits()),
		            				result.getTotalHits()}).getObject();
		            }
		            else
		            {
		                return new StringResourceModel(RI_NO_RESULTS_NIENTE, SearchResultMessagePanel.this, null).getObject();
		            }
		        }
			}
    	};
    }

}
