import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* 
 * Classe Executor
 * 
 * Cette classe permet d'exécuter des réactions chimiques en communiquant avec des serveurs distants.
 * Elle analyse les arguments de la ligne de commande pour obtenir les réactions et les adresses des machines,
 * puis envoie les réactions aux serveurs et traite les réponses.
 * 
 */
public class Executor {
    /*     * Méthode main
     * 
     * @param args : arguments de la ligne de commande
     */
    public static void main(String[] args) {
        List<String> reactions = new ArrayList<>();
        String machineAddress = "";

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.startsWith("--")) {
                if (i + 1 >= args.length) {
                    System.err.println("Erreur: L'indicateur " + arg + " nécessite une valeur.");
                    return;
                }

                String value = args[i + 1];

                switch (arg) {
                    case "--reaction":
                        reactions.add(value);
                        break;
                    case "--machine":
                        machineAddress = machineAddress + "/" + value;
                        break;
                    default:
                        System.err.println("Avertissement: Indicateur inconnu ignoré: " + arg);
                        break;
                }
                i++;
            } else {
                System.err.println("Avertissement: Argument non reconnu ignoré: " + arg);
            }
        }

        String[] tab_machineAddress = machineAddress.split("/");
        String message = "";

        for (String r : reactions) {
            message = message + r + "/";
        }

        System.out.println(message);

        // Build a dynamic list of host:port pairs, ignoring empty entries
        java.util.List<String[]> macList = new ArrayList<>();
        for (String e : tab_machineAddress) {
            if (e == null || e.trim().isEmpty()) {
                continue;
            }
            System.out.println(e);
            String[] parts = e.split(":", 2);
            if (parts.length == 2) {
                macList.add(parts);
            } else {
                System.err.println("Adresse machine invalide (attendu host:port): " + e);
            }
        }

        String[][] mac = macList.toArray(new String[0][]);
        if (mac.length > 0) {
            for (int i = 0; i < mac.length; i++) {
                System.out.println(" host: " + mac[i][0] + " port: " + mac[i][1]);
            }
        } else {
            System.err.println("Aucune adresse machine fournie.");
            return;
        }
        for (String r : reactions) {
            System.out.println(" reaction: " + r);
            if(checkreaction(r, mac, message)){
                System.out.println("Reaction " + r + " processed successfully.");
            } else {
                System.out.println("Reaction " + r + " could not be fully processed.");
            }
        }
        // Example call to checkreaction (optional):
        // boolean ok = checkreaction(reactions.isEmpty() ? "" : reactions.get(0), mac, message);
    }
    /*     * Méthode checkreaction
     * 
     * @param reaction : chaîne de caractères représentant la réaction à vérifier
     * @param mac : tableau 2D contenant les adresses machines (host et port)
     * @param message : message à envoyer aux serveurs
     * @return true si toutes les réactions ont été traitées, false sinon
     */
    static boolean checkreaction(String reaction, String[][] mac, String message) {
        if (mac == null || mac.length == 0) {
            System.err.println("Aucune adresse machine fournie pour checkreaction.");
            return false;
        }

        List<Integer> results = new ArrayList<>();
        Map<String, Integer> reactionMap = parseur(reaction);
        Map<String, Integer> finalReactionMap = new HashMap<>();

        for (int i = 0; i < mac.length; i++) {
            String host = mac[i][0];
            int port;
            try {
                port = Integer.parseInt(mac[i][1]);
            } catch (NumberFormatException ex) {
                System.err.println("Port invalide pour " + host + ": " + mac[i][1]);
                continue;
            }

            try (Socket s = new Socket(host, port);
                 ObjectOutputStream output = new ObjectOutputStream(s.getOutputStream());
                 ObjectInputStream input = new ObjectInputStream(s.getInputStream())) {

                // send the reaction string (or any protocol you expect)
                output.writeObject("LIST " + reaction);
                System.out.println("Connexion au Serveur [OK]");
                System.out.println("Message sent: \"" + message + "\"");

                Object obj = input.readObject();
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> reponse = (Map<String, Integer>) obj;
                    for (String e : reponse.keySet()) {
                        if (reactionMap.containsKey(e)) {
                            results.add(i);
                            reactionMap.remove(e);
                            finalReactionMap.put(e, reponse.get(e));
                        }
                    }
                    System.out.println("Message reçu: \"" + reponse + "\"");
                } else {
                    System.err.println("Réponse inattendue du serveur: " + obj);
                }

            } catch (IOException | ClassNotFoundException E) {
                System.err.println("Erreur Client : Impossible de se connecter ou de communiquer avec le serveur.");
                System.err.println("Détails : " + E.getMessage());
                return false;
            }
            HashSet<Integer> hashSet = new HashSet<>(results);
            results.clear();
            results.addAll(hashSet);
            if (reactionMap.isEmpty()) {
                // send the finalReactionMap back to servers that matched
                for (int n : results) {
                    try (Socket s2 = new Socket(mac[n][0], Integer.parseInt(mac[n][1]));
                         ObjectOutputStream output2 = new ObjectOutputStream(s2.getOutputStream())) {

                        output2.writeObject(finalReactionMap);

                    } catch (IOException E) {
                        System.err.println("Erreur Client : Impossible de se connecter ou de communiquer avec le serveur.");
                        System.err.println("Détails : " + E.getMessage());
                        return false;
                    }
                }
                return true;
            }
        }

        return false;
    }
    /*     * Méthode parseur
     * 
     * @param s : chaîne de caractères à analyser
     * @return une Map avec les lettres comme clés et les chiffres comme valeurs
     */
    public static Map<String, Integer> parseur(String s) {
        Pattern pattern = Pattern.compile("(\\d+)([A-Z])");
        Map<String, Integer> res = new HashMap<>();

        if (s == null) {
            return res;
        }

        Matcher match = pattern.matcher(s);
        while (match.find()) {
            String lettre = match.group(2);
            int chiffre = Integer.parseInt(match.group(1));
            res.put(lettre, chiffre);
        }
        return res;
    }

}
