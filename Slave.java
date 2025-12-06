import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Slave implements Runnable {
    private Socket client;
    private ArrayList<Machine> machines;
    
    public Slave(Socket client, ArrayList<Machine> machines){
        this.client = client;
        this.machines = machines;
    }

    public Socket getClient(){
        return this.client;
    }

    public void run() {
        try{
            System.out.println("[START] new client.");
            ObjectInputStream input_client = new ObjectInputStream(client.getInputStream());
            ObjectOutputStream output_client = new ObjectOutputStream(client.getOutputStream());
            String entry = ((String) input_client.readObject());
            
            
            //Ajoute des "1" devant chaque lettre si recoit A + 2C -> D
            String regex = "(?<!\\d)([A-Z]+)";
            String messagePrepare = entry.replaceAll(regex, "1$1");

            //Traite chaque regles un par un, en partant du principe que / est notre separateur
            String[] messages = messagePrepare.split("/");
            for (String message : messages){
                //Convertis la regles en Map <String, Integer>
                Map<String, Map<String, Integer>> termes = parse_msg(message);

                Map<String, Integer> consommables = termes.get("Besoin");
                Map<String, Integer> produits = termes.get("Resultat");
                
                Map<String, Machine> bosseur = get_Machine_Travail(consommables);
                Map<String, Machine> receveur = get_Machine_Recoit(produits);
                //System.out.println(receveur);
                //System.out.println(bosseur);
                if(bosseur == null || receveur == null){
                    System.out.println(message + " abort");
                    output_client.writeUTF(message + " abort");
                }
                else{
                    output_client.writeUTF("Ok");
                    System.out.println(message + " Ok");
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }

    //Fonction qui donne une liste de machine + quel besoin il s'occupe
    public Map<String, Machine> get_Machine_Travail(Map<String, Integer> consommables){
        Map<String, Machine> total = new HashMap<>();
        String ressouPro;
        for(Machine m : machines){
            ressouPro = get_ressources_produce(consommables, m);
            if(ressouPro != null){
                total.put(ressouPro, m);
                consommables.remove(ressouPro);
            }
            if(consommables.size() == 0){
                break;
            }
        }
        //System.out.println("Machine");
        //System.out.println(total);
        if(total.size() == 0 || consommables.size() > 0){
            return null;
        }
        return total;
    }

    public Map<String, Machine> get_Machine_Recoit(Map<String, Integer> production){
        Map<String, Machine> total = new HashMap<>();

        for(Machine m : machines){
            for (Map.Entry<String, Integer> entree : production.entrySet()) {
                String ressource = entree.getKey();
                if(m.is_ressources_in(ressource) != 0){ 
                    if (!total.containsKey(ressource)) {
                        total.put(ressource, m);
                    }
                }
            }
        }
        if(total.size() < production.size()){
            return null;
        }
        return total;
    }

    //Renvoie le nom d'une ressource si elle peut etre traiter
    public String get_ressources_produce(Map<String, Integer> besoins, Machine machine){
        for (Map.Entry<String, Integer> entree : besoins.entrySet()) {
            String ressource = entree.getKey();
            int nb_dispo = machine.is_ressources_in(ressource);
            if(nb_dispo > 0 && nb_dispo >= besoins.get(ressource)){ 
                return ressource;
            }
        }
        return null;
    }


    //Parsing des regles
    public Map<String, Map<String, Integer>> parse_msg(String entry) throws IllegalArgumentException{
        String[] message = entry.split("->");
        if(message.length != 2){
            throw new IllegalArgumentException("Format pas bon\nBon format : 3A + 2C -> 4A");
        }
        Map<String, Integer> consommables = parseur(message[0].trim());
        Map<String, Integer> produits = parseur(message[1].trim());
  
        Map<String, Map<String, Integer>> res = new HashMap<>();
        res.put("Besoin", consommables);
        res.put("Resultat", produits);
        return res;
    }

    public static Map<String, Integer> parseur(String s){
        Pattern pattern = Pattern.compile("(\\d+)([A-Z])");
        Map<String, Integer> res = new HashMap<>();

        Matcher match = pattern.matcher(s);
        while (match.find()){
            String lettre = match.group(2);
            int chiffre = Integer.parseInt(match.group(1));
            res.put(lettre, chiffre);
        }
        return res;
    }
}
