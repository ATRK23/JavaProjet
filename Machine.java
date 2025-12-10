import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
* Classe Machine représentant une machine qui gère des ressources
* ressources : Map des ressources de la machine
* pool : pool de threads pour gérer les connexions entrantes
* port : port de la machine
* poolSize : taille du pool de threads
* isFinished : indique si la machine doit arrêter de gérer les connexions
* server : ServerSocket pour écouter les connexions entrantes
*/

public class Machine{
    /* Attributs */
    private Map< String,Integer> ressources;
    private ExecutorService pool;
    private int port;
    private int poolSize=6;
    private boolean isFinished;
    private ServerSocket server;

    /*
    * Constructeur de la classe Machine
    * @param port : port de la machine
    * @param poolSize : taille du pool de threads
    */
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

    //Getters
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
    public synchronized Map<String,Integer> get_ressources(){
        return this.ressources;
    }

    /*
    * Méthode createRessource
    * Crée une ressource res avec une quantité nb_res dans la machine
    * @param nb_res : quantité de la ressource
    * @param res : ressource à créer
    */
    public synchronized void createRessource(int nb_res,String res){
        int nb = nb_res; 
        if(nb < 0){
            nb = 0;
        }
        this.ressources.put(res, nb);
    }

    //Methodes de modification des ressources
    /*
    * Méthode addRessource
    * Ajoute une quantité nb_res de la ressource res à la machine
    * @param nb_res : quantité à ajouter
    * @param res : ressource à ajouter
    * @throws Exception si nb_res est négatif
    */
    public synchronized void addRessource(int nb_res,String res) throws Exception {
        if(nb_res<0){
            throw new Exception("cannot add negative value");        
        } else {
            ressources.put(res, ressources.get(res)+nb_res);
        }
    }

    /*
    * Méthode removeRessource
    * Retire une quantité nb_res de la ressource res de la machine
    * @param nb_res : quantité à retirer
    * @param res : ressource à retirer
    * @throws Exception si la quantité à retirer est supérieure à la quantité disponible
    */
    public synchronized void removeRessource(int nb_res,String res) throws Exception{
        if (ressources.get(res)-nb_res>=0){
            ressources.put(res, ressources.get(res)-nb_res);
        }else{
            throw new Exception("argument nb cannot be negative");
        }
    }

    //Methodes de vérification des ressources
    /*
    * Méthode is_ressources_in
    * Vérifie la quantité disponible de la ressource s dans la machine
    * @param s : ressource à vérifier
    * @return la quantité disponible de la ressource s, ou -1 si la ressource n'existe pas
    */
    public int is_ressources_in(String s){
        if(ressources.containsKey(s)){
            return ressources.get(s);
        }
        else{return -1;}
    }

    /*
    * Méthode has_ressource
    * Vérifie si la machine possède au moins n unités de la ressource s
    * @param s : ressource à vérifier
    * @param n : quantité minimale requise
    * @return true si la machine possède au moins n unités de la ressource s, false sinon
    */
    public boolean has_ressource(String s, int n){
        if(ressources.containsKey(s)){
            return ressources.get(s) >= n;
        }
        else{return false;}
    }

    /*
    * Méthode can_Receive
    * Vérifie si la machine peut recevoir une ressource donnée 
    * @param s : ressource à vérifier
    * @return true si la machine peut recevoir la ressource, false sinon
    */
    public boolean can_Receive(String s){
        if(this.has_ressource(s, 0)) return true;
        return false;
    }

    /*
    * Méthode manageRequest
    * Gère les requêtes entrantes des executors
    */
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

    /*
    * Méthode parseur
    * Convertis une chaîne de caractères en une Map<String, Integer>
    * Les lettres représentent les ressources et les chiffres représentent les quantités
    * 
    * @param s : chaîne de caractères à analyser
    * @return une Map avec les lettres comme clés (ressource) et les chiffres comme valeurs (quantité)
    */
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

