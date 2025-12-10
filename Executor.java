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
import java.util.logging.Level;
import java.util.logging.Logger;

/* 
 * Classe Executor
 * 
 * Cette classe permet d'exécuter des réactions chimiques en communiquant avec des serveurs distants.
 * Elle analyse les arguments de la ligne de commande pour obtenir les réactions et les adresses des machines,
 * puis envoie les réactions aux serveurs et traite les réponses.
 * 
 */
public class Executor {
    private static final Logger LOGGER = Logger.getLogger(Executor.class.getName());

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
                    LOGGER.severe("[EXECUTOR] Erreur: L'indicateur " + arg + " nécessite une valeur.");
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
                        LOGGER.warning("[EXECUTOR] Avertissement: Indicateur inconnu ignoré: " + arg);
                        break;
                }
                i++;
            } else {
                LOGGER.warning("[EXECUTOR] Avertissement: Argument non reconnu ignoré: " + arg);
            }
        }

        String[] tab_machineAddress = machineAddress.split("/");
        String message = "";

        for (String r : reactions) {
            message = message + r + "/";
        }

        LOGGER.info("[EXECUTOR] Réactions agrégées: " + message);

        // Build a dynamic list of host:port pairs, ignoring empty entries
        java.util.List<String[]> macList = new ArrayList<>();
        for (String e : tab_machineAddress) {
            if (e == null || e.trim().isEmpty()) {
                continue;
            }
            LOGGER.info("[EXECUTOR] Machine ciblée : " + e);
            String[] parts = e.split(":", 2);
            if (parts.length == 2) {
                macList.add(parts);
            } else {
                LOGGER.warning("[EXECUTOR] Adresse machine invalide (attendu host:port): " + e);
            }
        }

        String[][] mac = macList.toArray(new String[0][]);
        if (mac.length > 0) {
            for (int i = 0; i < mac.length; i++) {
                LOGGER.info("[EXECUTOR] Machine configurée -> host: " + mac[i][0] + " port: " + mac[i][1]);
            }
        } else {
            LOGGER.severe("[EXECUTOR] Aucune adresse machine fournie.");
            return;
        }
        for (String r : reactions) {
            LOGGER.info("[EXECUTOR] Reaction à traiter: " + r);
            if(checkreaction(r, mac, message)){
                LOGGER.info("[EXECUTOR] Reaction " + r + " processed successfully.");
            } else {
                LOGGER.warning("[EXECUTOR] Reaction " + r + " could not be fully processed.");
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
            LOGGER.severe("[EXECUTOR] Aucune adresse machine fournie pour checkreaction.");
            return false;
        }

        List<Integer> results = new ArrayList<>();
        Map<String, Integer> reactionMap = parseur(reaction);
        LOGGER.info("[EXECUTOR] reactionMap:" + reactionMap);
        Map<String, Integer> finalReactionMap = new HashMap<>();

        for (int i = 0; i < mac.length; i++) {
            String host = mac[i][0];
            int port;
            try {
                port = Integer.parseInt(mac[i][1]);
            } catch (NumberFormatException ex) {
                LOGGER.warning("[EXECUTOR] Port invalide pour " + host + ": " + mac[i][1]);
                continue;
            }

            try (Socket s = new Socket(host, port);
                 ObjectOutputStream output = new ObjectOutputStream(s.getOutputStream());
                 ObjectInputStream input = new ObjectInputStream(s.getInputStream())) {

                // send the reaction string (or any protocol you expect)
                output.writeObject("LIST " + reaction);
                LOGGER.info("[EXECUTOR] Connexion au Serveur [OK]");
                LOGGER.info("[EXECUTOR] Message sent: \"" + reaction + "\"");

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
                    LOGGER.info("[EXECUTOR] Message reçu: \"" + reponse + "\"");
                } else {
                    LOGGER.warning("[EXECUTOR] Réponse inattendue du serveur: " + obj);
                }

            } catch (IOException | ClassNotFoundException E) {
                LOGGER.log(Level.SEVERE, "[EXECUTOR] Erreur Client : Impossible de se connecter ou de communiquer avec le serveur.", E);
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
                        LOGGER.log(Level.SEVERE, "[EXECUTOR] Erreur Client : Impossible de se connecter ou de communiquer avec le serveur.", E);
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
        String regex = "(?<!\\d)([A-Z]+)";
        String messagePrepare = s.replaceAll(regex, "1$1");
        Pattern pattern = Pattern.compile("(\\d+)([A-Z])");
        Map<String, Integer> res = new HashMap<>();

        if ( messagePrepare == null) {
            return res;
        }

        Matcher match = pattern.matcher(messagePrepare);
        while (match.find()) {
            String lettre = match.group(2);
            int chiffre = Integer.parseInt(match.group(1));
            res.put(lettre, chiffre);
        }
        return res;
    }

}
