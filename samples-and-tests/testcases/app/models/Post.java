package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import play.db.jpa.Model;

@Entity
public class Post extends Model {

    public String content;
    public Date postedAt;
    
    @ManyToOne
    public Topic topic;

}

