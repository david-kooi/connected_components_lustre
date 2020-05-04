import java.util.HashSet;


class ConnectedComponentSet{
    HashSet<ConnectedComponent> CC = new HashSet<>();

    public ConnectedComponentSet(HashSet<ConnectedComponent> CC){
        this.CC = CC;
    }


    public void print(){
        for(ConnectedComponent C : this.CC){
            System.out.println("-- Connected Component --");
            System.out.println("Properties");
            System.out.println(C.properties);
            System.out.println("Outputs");
            System.out.println(C.outputs);
            System.out.println("-------------------------");
        }
    }

}
