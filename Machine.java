import java.io.*;
import java.net.*;
import java.util.HashMap;
//import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Machine{
    private Map< String,Integer> ressources;
    
    private ExecutorService pool;
    private int port;
    private int poolSize=6;
    private boolean isFinished;
    private ServerSocket server;

    public Machine(int port, int poolSize){
        this.ressources = new HashMap<>();

        this.port = port;
        this.poolSize = poolSize;
        this.isFinished =false;
        try{
            this.server = new ServerSocket(this.port);
        } catch (IOException E){
            E.printStackTrace();
        }
        this.pool = Executors.newFixedThreadPool(this.poolSize);
    }
    public synchronized Map<String,Integer> get_ressources(){
        return this.ressources;
    }

    public synchronized void createRessource(int nb_res,String res){
        int nb = nb_res; 
        if(nb < 0){
            nb = 0;
        }
        this.ressources.put(res, nb);
    }
    public synchronized void addRessource(int nb_res,String res) throws Exception {
        if(nb_res<0){
            throw new Exception("cannot add negative value");        
        } else {
            ressources.put(res, ressources.get(res)+nb_res);
        }
    }
    public synchronized void removeRessource(int nb_res,String res) throws Exception{
        if (ressources.get(res)-nb_res>=0){
            ressources.put(res, ressources.get(res)-nb_res);
        }else{
            throw new Exception("argument nb cannot be negative");
        }
    }
    public int is_ressources_in(String s){
        if(ressources.containsKey(s)){
            return ressources.get(s);
        }
        else{return -1;}
    }
    public boolean has_ressource(String s, int n){
        if(ressources.containsKey(s)){
            return ressources.get(s) >= n;
        }
        else{return false;}
    }
    public boolean can_Receive(String s){
        if(this.has_ressource(s, 0)) return true;
        return false;
    }

    public int getPort(){
        return this.port;
    }

    public int getPoolSize(){
        return this.poolSize;
    }

    public boolean getIsFinished(){
        return this.isFinished;
    }

    public ExecutorService getPool(){
        return this.pool;
    }

    public void manageRequest(){
        try {
            System.out.println("Waiting for connections...");
            while(!isFinished){
                this.pool.execute(new Slave(server.accept(), this));
            }

        } catch (IOException E) {
            E.printStackTrace();
        }
    }

    public static Map<String, Integer> parseur(String s){
        String regex = "(?<!\\d)([A-Z]+)";
        String messagePrepare = s.replaceAll(regex, "1$1");

        Pattern pattern = Pattern.compile("(\\d+)([A-Z])");
        Map<String, Integer> res = new HashMap<>();

        Matcher match = pattern.matcher(messagePrepare);
        while (match.find()){
            String lettre = match.group(2);
            int chiffre = Integer.parseInt(match.group(1));
            res.put(lettre, chiffre);
        }
        return res;
    }

    public static void main(String[] args){
        int port = 0;
        if(args.length != 2){
            System.err.println("Usage : java Machine <port> <ressources> \n#Example : java Machine 30000 3A/2B/0D");
            return;
        }
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            System.err.println("NumberFormatException, port invalide");
            return;
        }

        Map<String, Integer> ressources = parseur(args[1]);
        if(ressources == null){
            System.err.println("Usage : java Machine <port> <ressources> \n#Example : java Machine 30000 3A/2B/0D");
            return;
        }

        try {
            Machine a = new Machine(port, 6);
            System.out.println("Machine au port : " + port);
            for (Map.Entry<String, Integer> res : ressources.entrySet()) {
                String ressource = res.getKey();
                int nb_dispo = res.getValue();
                a.createRessource(nb_dispo, ressource);
                System.out.println("Ajouté : "+ nb_dispo + ressource);
            }
            a.manageRequest();
        } catch (Exception e){
            System.err.println(e.getMessage());
        }
    }
} 

