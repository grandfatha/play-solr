package play.modules.solr;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityListeners;

//import org.apache.log4j.Logger;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.hibernate.impl.SessionFactoryImpl;

import play.Play;
import play.PlayPlugin;
import play.templates.JavaExtensions;

public class SolrPlugin extends PlayPlugin {
	
	private static final Logger log = Logger.getLogger(SolrPlugin.class);
	
	// Configuration Properties for this plugin
	private static final String SOLR_URL_PROP_KEY = "play-solr.url";
	private static final String DISABLE_AUTO_CONFIG_KEY = "play-solr.disable-jpalistener-autoconfig";
	
	// Hibernate Config Properties
	private static final String POST_INSERT = "hibernate.ejb.event.post-insert";
	private static final String POST_UPDATE = "hibernate.ejb.event.post-update";
	private static final String POST_DELETE = "hibernate.ejb.event.post-delete";

	private static final String SOLR_LISTENER_CLS = "play.modules.solr.SolrEntityListener";
	
	
	@Override
	public void onApplicationStart() {
	
		String url = Play.configuration.getProperty(SOLR_URL_PROP_KEY);
		
		if(url == null){
			log.error(String.format("No Solr-Url found, did you add a valid url using [%s] in application.conf ?", SOLR_URL_PROP_KEY));
		}
		else{
			try {
	
				CommonsHttpSolrServer server = new CommonsHttpSolrServer(url);
				log.debug(String.format("Trying to access Solr-Server at URL: %s", url));
				SolrPingResponse ping = server.ping();
				log.debug(String.format("Got response from Solr: %s", ping.toString()));
				
				Solr.server = server;
				
			} catch (Exception e) {
				log.error("Failure during instantiation of the Solr-Plugin", e);
			}
		}
	}
	
	
	@Override
	public void onConfigurationRead() {
		
		// in case somebody wants to explicitly configure the JPA listeners himself
		String disableHackery = Play.configuration.getProperty(DISABLE_AUTO_CONFIG_KEY);
		if(!Boolean.parseBoolean(disableHackery)){
			
			// listen to save events
			String givenCfg = Play.configuration.getProperty(POST_INSERT);
			manageLifecycleProperty(givenCfg, POST_INSERT, SOLR_LISTENER_CLS);
			
			// listen to updates
			givenCfg = Play.configuration.getProperty(POST_UPDATE);
			manageLifecycleProperty(givenCfg, POST_UPDATE, SOLR_LISTENER_CLS);
			
			// listen to deletes
			givenCfg = Play.configuration.getProperty(POST_DELETE);
			manageLifecycleProperty(givenCfg, POST_DELETE, SOLR_LISTENER_CLS);
			
		}
	}
	
	private void manageLifecycleProperty(String given, String target, String value){
		
		if(given != null && given.length() > 0){
			// there is already a configuration for the lifecycle-event, lets not screw it up
			
			List<String> classes = Arrays.asList(given.trim().split(","));
			
			// make sure the configuration does not include our own listener
			// otherwise we add it twice here...
			if(!classes.contains(value)){
				classes.add(value);
				Play.configuration.setProperty(target, JavaExtensions.join(classes, ","));
			}
		}
		else{
			// default case: just set our listener to the given jpa lifecycle event
			Play.configuration.setProperty(target, value);
		}
	}
	
}
