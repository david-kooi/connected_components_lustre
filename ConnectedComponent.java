import java.util.Set;
import java.util.HashSet;

import jkind.slicing.Dependency;



class ConnectedComponent{
    public HashSet<String> properties = new HashSet<>();;
    public HashSet<Dependency> outputs = new HashSet<>();

    public ConnectedComponent(){

    }

    public ConnectedComponent(String property, HashSet<Dependency> outputs){
        this.properties.add(property);
        this.outputs.addAll(outputs);
    }
} 
