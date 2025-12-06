import java.util.HashMap;
import java.util.Map;

public class Machine{
    private Map< String,Integer> ressources;
    public Machine(){
        this.ressources = new HashMap<>();
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
    public boolean is_ressources_in(String s){
        return ressources.containsKey(s);
    }
} 