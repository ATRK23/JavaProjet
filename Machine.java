import java.util.ArrayList;
import java.util.HashMap;
//import java.util.List;
import java.util.Map;

public class Machine{
    private Map< String,Integer> ressources;
    private static ArrayList<Machine> database = new ArrayList<>();


    public Machine(){
        this.ressources = new HashMap<>();
        database.add(this);
    }
    public synchronized Map<String,Integer> get_ressources(){
        return this.ressources;
    }
    public synchronized void createRessource(int nb_res,String res){
        this.ressources.put(res, nb_res);
    }
    public synchronized void addRessource(int nb_res,String res){
        ressources.put(res, ressources.get(res)+nb_res);
    }
    public synchronized void removeRessource(int nb_res,String res) throws Exception{
        if (ressources.get(res)-nb_res>=0){
            ressources.put(res, ressources.get(res)-nb_res);
        }else{
            throw new Exception("argument nb cannot be negative");
        }
    }
    public static void addMachine(Machine machine) {
        database.add(machine);
    }
    public int is_ressources_in(String s){
        if(ressources.containsKey(s)){
            return ressources.get(s);
        }
        else{return 0;}
    }
    public static ArrayList<Machine> getDatabase() {
        return database;
    }

} 

