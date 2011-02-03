package play.modules.solr;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;

import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.db.jpa.JPA;

public class Solr {
	
	public static String UPDATE_EXTRACT_HANDLER_URL = "/update/extract";

	private static final Logger log = Logger.getLogger(Solr.class);
	
	public static CommonsHttpSolrServer server;
	
	public static QueryResponse query(String query){
		if(query != null && query.length() > 0 ){
			return query(new SolrQuery(query));
		}
		else {
			return null;
		}
		
	}
	
	public static QueryResponse query(SolrQuery query){
		
		QueryResponse resp = null;
		try {
			resp = server.query(query);
		} catch (SolrServerException e) {
			log.error(String.format("Failure during Solr-Query-Operation for [%s]", query.toString()),e);
		}
		return resp;
	}
	
	public static void delete(final Object o) {
		
		if(SolrMapper.isSolrSearchable(o)){
			
			log.debug(String.format("Removing Entity [%s] from Solr", o.toString()));
			
			try {
				new DeleteJob(server, o).now();
			} catch (Exception e) {
				log.error(String.format("Failure during Solr-Delete-Operation for object [%s]", o.toString()),e);
			}
		}
	}
	
	public static void deleteAll(){
		try {
			new DeleteJob(server, "*:*").now();
		} catch (Exception e) {
			log.error("Failure during Solr-Delete-All-Operation", e);
		}
	}
	
	public void indexAllModels() {

		List<ApplicationClass> classes = Play.classes.getAnnotatedClasses(SolrSearchable.class);
		
		for (ApplicationClass cls : classes) {
			if(cls.javaClass.isAnnotationPresent(SolrSearchable.class)
					&& cls.javaClass.isAnnotationPresent(Entity.class)){
				
				Query query = JPA.em().createQuery("from "+cls.name);
				
				List<Object> result = query.getResultList();
				
				save(result);
			}
		}
		
	}

	public static void save(Object o) {
		
		if(SolrMapper.isSolrSearchable(o)){
			
			log.debug(String.format("Saving Entity [%s] into Solr", o.toString()));
			
			try {
				
				SolrInputDocument doc = SolrMapper.convertToSolrDoc(o);
				new SaveJob(server, doc).now();
				
			} catch (Exception e) {
				log.error(String.format("Failure during SOlr-Save-Operation for object [%s]", o.toString()),e);
			}
		}
	}
	
	public static void save(Collection<Object> objs){
		
		List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
		
		try {
			for (Object o : objs) {
				docs.add(SolrMapper.convertToSolrDoc(o));
			}
			new SaveJob(server, docs).now();
			
		} catch (Exception e) {
			log.error("Failure during Solr-Bulk-Save-Operation",e);
		}
	}
	
	public static NamedList<Object> save(File file){
		return save(Arrays.asList(file), new HashMap<String,String>(0));
	}
	
	public static NamedList<Object> save(Collection<File> files){
		return save(files, new HashMap<String,String>(0));
	}
	
	public static NamedList<Object> save(Collection<File> files, Map<String, String> params){
		
		NamedList<Object> result = null;
		
		log.debug(String.format("Trying to save Files [%s] into Solr", files));
		
		if(files == null || files.isEmpty()){
			log.debug("No files to add.");
			return result;
		}
	   
	    try {
	    	
	    	ContentStreamUpdateRequest req = new ContentStreamUpdateRequest(UPDATE_EXTRACT_HANDLER_URL);
	    	
	    	for (File file : files) {
	    		req.addFile(file);
			}
			
			for (Map.Entry<String, String> entry : params.entrySet()) {
				req.setParam(entry.getKey(), entry.getValue());
			}
			result = server.request(req);
		} catch (Exception e) {
			log.error(String.format("Failure while File-Save-Operation for Files [%s]", files),e);
		}
	    
	    return result;
	}
}
