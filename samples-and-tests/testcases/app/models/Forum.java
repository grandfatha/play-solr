package models;

import javax.persistence.*;
import java.util.*;

import play.db.jpa.*;
import play.data.validation.*;

@Entity
public class Forum extends Model {

    @Required
    public String name;
    public String description;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "forum")
    public List<Topic> topics;
    
}

