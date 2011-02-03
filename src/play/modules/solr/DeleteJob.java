/**
 * 
 */
package play.modules.solr;

import org.apache.solr.client.solrj.SolrServer;

import play.jobs.Job;

class DeleteJob extends Job{
	
	private SolrServer server;
	private Object obj;
	private String query;

	public DeleteJob(SolrServer server, Object o){
		this.server = server;
		this.obj = o;
	}
	
	public DeleteJob(SolrServer server, String q){
		this.server = server;
		this.query = q;
	}
	
	@Override
	public void doJob() throws Exception {
		
		if(obj != null){
			
			String key;
			if( (key = SolrMapper.getJPAKey(obj)) != null){
				server.deleteById(SolrMapper.buildIdField(obj, key));
				server.commit();
			}
		}
		
		if(query != null){
			server.deleteByQuery(query);
			server.commit();
		}
		
	}
}