package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import play.data.validation.Required;
import play.db.jpa.Model;
import play.modules.solr.SolrField;
import play.modules.solr.SolrSearchable;

@Entity
@SolrSearchable
public class Topic extends Model {

    @Required
    @SolrField
    public String subject;
    
    public Integer views = 0;
    
    @ManyToOne
    public Forum forum;
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "topic")
    public List<Post> posts = new ArrayList<Post>(0);
    
    @SolrField
    public String computeSomethingHere(){
    	return "this-is-just-for-the-testcase";
    }
 
}

