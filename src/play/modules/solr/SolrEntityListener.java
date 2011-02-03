package play.modules.solr;

import org.apache.log4j.Logger;
import org.hibernate.event.PostDeleteEvent;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener;

import play.Play;

public class SolrEntityListener implements PostDeleteEventListener, PostInsertEventListener, PostUpdateEventListener{

	@Override
	public void onPostDelete(PostDeleteEvent evt) {
		Solr.delete(evt.getEntity());
	}

	@Override
	public void onPostInsert(PostInsertEvent evt) {
		Solr.save(evt.getEntity());
	}

	@Override
	public void onPostUpdate(PostUpdateEvent evt) {
		Solr.save(evt.getEntity());
	}

}
