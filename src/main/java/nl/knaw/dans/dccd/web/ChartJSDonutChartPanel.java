package nl.knaw.dans.dccd.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.panel.Panel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChartJSDonutChartPanel extends Panel {

	private static final long serialVersionUID = 1L;
;
	private static Logger logger = LoggerFactory.getLogger(ChartJSDonutChartPanel.class);
	private String jsonContents;
	private String thisid;

	public ChartJSDonutChartPanel(String id, File file) 
	{
		super(id);
		thisid = id;
		logger.info("Instantiating ChartJSDonutChartPanel");

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));

		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    jsonContents = sb.toString();
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		} finally {
		    try {
				br.close();
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
		
		add(new ChartJSDonutChartPanelBehaviour());
		
		
	}
	
	class ChartJSDonutChartPanelBehaviour extends AbstractBehavior
	{

		private static final long serialVersionUID = 1L;

		@Override
		public void renderHead(IHeaderResponse response)
		{
			logger.debug("DonutChartPanel - renderHead");
			
			response.renderJavascriptReference("http://ajax.googleapis.com/ajax/libs/jquery/1.9.0/jquery.min.js");
			response.renderJavascriptReference("http://cdnjs.cloudflare.com/ajax/libs/raphael/2.1.0/raphael-min.js");
			response.renderJavascriptReference("http://cdnjs.cloudflare.com/ajax/libs/morris.js/0.5.1/morris.min.js");
			
			String script = "Morris.Donut({ element: '"+thisid+"', data: "+ jsonContents + "});";
			response.renderOnDomReadyJavascript(script);
			super.renderHead(response);
		}		
	}
}
