package nl.knaw.dans.dccd.web;

import java.io.File;
import java.util.Properties;

import nl.knaw.dans.common.lang.user.User;
import nl.knaw.dans.dccd.application.services.DccdConfigurationService;

import org.apache.wicket.markup.html.panel.Panel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsDisplayPanel extends Panel {
	private static Logger logger = LoggerFactory.getLogger(StatisticsDisplayPanel.class);
	public static final String PROJECT_CATEGORIES_PATH = "project.categories.path";
	public static final String TAXON_DATA_PATH = "taxon.data.path";
	private static final long serialVersionUID = -1876178855794288942L;
	User currentUser;


	public StatisticsDisplayPanel(String id) {
		super(id);
		init();
	}
	
	
	private void init()
	{
		
		currentUser = ((DccdSession) getSession()).getUser();
		
		if(currentUser!=null){
			initCategoriesChart();
			initTaxonChart();
		}
	}

	
	private void initCategoriesChart()
	{
		logger.error("initCategoriesChart()");

		// TODO get file path/name from configuration
		Properties settings = DccdConfigurationService.getService().getSettings();
		String filePath = settings.getProperty(PROJECT_CATEGORIES_PATH);
		File file = null;
		if (filePath == null) 
		{
			// no file to read, bail out
			logger.info("No categories read from file because No property found for: " + PROJECT_CATEGORIES_PATH);
		}
		else
		{
			file = new File(filePath);
			logger.info("Read categories file from " + PROJECT_CATEGORIES_PATH);

		}
		
		MorrisDonutChartPanel chart = new MorrisDonutChartPanel("categoriesChart", file);
		add(chart);	    
	}    

	
	private void initTaxonChart()
	{
		logger.error("initTaxonChart()");

		// TODO get file path/name from configuration
		Properties settings = DccdConfigurationService.getService().getSettings();
		String filePath = settings.getProperty(TAXON_DATA_PATH);
		File file = null;
		if (filePath == null) 
		{
			// no file to read, bail out
			logger.info("No taxon info read from file because No property found for: " + TAXON_DATA_PATH);
		}
		else
		{
			file = new File(filePath);
			logger.info("Read taxon data file from " + TAXON_DATA_PATH);

		}
		
		MorrisDonutChartPanel chart = new MorrisDonutChartPanel("taxonChart", file);
		add(chart);	    
	}    
	
}
