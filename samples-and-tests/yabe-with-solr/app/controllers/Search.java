package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

import play.modules.solr.*;
import org.apache.solr.client.solrj.response.QueryResponse;


public class Search extends Controller {

	@Before
    static void addDefaults() {
        renderArgs.put("blogTitle", Play.configuration.getProperty("blog.title"));
        renderArgs.put("blogBaseline", Play.configuration.getProperty("blog.baseline"));
    }

	public static void query(String q) {
		
		renderArgs.put("query", q);
		renderArgs.put("resp", Solr.query(q));
		render();
	}
    
}

