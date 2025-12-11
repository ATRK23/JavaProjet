/**
 * @author Arthur BAILLET
 * @author Jacques ZHANG
 * @author Jessy BRIET
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class Test {
    private static final Logger LOGGER = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) {
        // Configure logger to write to a file `app.log`
        try {
            FileHandler fh = new FileHandler("app.log", true);
            fh.setFormatter(new SimpleFormatter());
            fh.setLevel(Level.ALL);
            LOGGER.addHandler(fh);
            LOGGER.setLevel(Level.ALL);
            LOGGER.setUseParentHandlers(false);
        } catch (IOException e) {
            System.err.println("Impossible d'ouvrir le fichier de log app.log: " + e.getMessage());
        }

        List<String[]> machines = new ArrayList<>();
        String executorArgs = null;
        String randomSpec = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--machine".equals(arg)) {
                if (i + 1 >= args.length) {
                    usage();
                    return;
                }
                machines.add(args[++i].trim().split("\\s+", 2));
            } else if ("--executor".equals(arg)) {
                if (i + 1 >= args.length) {
                    usage();
                    return;
                }
                executorArgs = args[++i];
            } else if ("--random".equals(arg)) {
                if (i + 1 >= args.length) {
                    usage();
                    return;
                }
                randomSpec = args[++i];
            } else {
                LOGGER.warning("[TEST] Argument inconnu ignoré : " + arg);
            }
        }

        // Si --random fourni, génère les machines et l'argument executor automatiquement
        if (randomSpec != null) {
            GeneratedConfig cfg = generateRandomFromSpec(randomSpec);
            machines = cfg.machines;
            executorArgs = cfg.executorArgs;
            LOGGER.info("[TEST] Généré via --random: machines=" + machines.size() + " executorArgs='" + executorArgs + "'");
        }

        if (machines.isEmpty() || executorArgs == null) {
            usage();
            return;
        }

        List<Process> machineProcesses = new ArrayList<>();
        try {
            for (String[] machine : machines) {
                if (machine.length < 2) {
                    LOGGER.severe("[TEST] Format machine invalide. Attendu : \"<port> <ressources>\"");
                    return;
                }
                List<String> cmd = Arrays.asList("java", "Machine", machine[0], machine[1]);
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.inheritIO();
                machineProcesses.add(pb.start());
            }

            // Laisse aux machines un court temps de démarrage avant de contacter l'executor
            Thread.sleep(300);

            // Lancer l'executor avec les arguments fournis
            List<String> execCmd = new ArrayList<>();
            execCmd.add("java");
            execCmd.add("Executor");
            execCmd.addAll(splitArgs(executorArgs));

            ProcessBuilder executorProcess = new ProcessBuilder(execCmd);
            executorProcess.inheritIO();
            Process exec = executorProcess.start();
            int code = exec.waitFor();
            LOGGER.info("[TEST] Executor terminé avec le code : " + code);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[TEST] Echec lors du lancement des processus", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "[TEST] Interruption lors de l'attente de l'executor", e);
        } finally {
            for (Process p : machineProcesses) {
                p.destroy();
            }
        }
    }

    // Container pour config générée
    private static class GeneratedConfig {
        List<String[]> machines;
        String executorArgs;

        GeneratedConfig(List<String[]> m, String e) { machines = m; executorArgs = e; }
    }

    // Parse une spécification globale en map lettre->quantité
    private static Map<Character,Integer> parseGlobalSpec(String spec) {
        Map<Character,Integer> counts = new HashMap<>();
        if (spec == null) return counts;
        if (spec.contains("/")) {
            for (String part : spec.split("/")) {
                part = part.trim();
                if (part.isEmpty()) continue;
                int idx = 0;
                while (idx < part.length() && Character.isDigit(part.charAt(idx))) idx++;
                String num = part.substring(0, idx);
                String letter = part.substring(idx);
                if (letter.isEmpty()) continue;
                int n = num.isEmpty() ? 1 : Integer.parseInt(num);
                char c = Character.toUpperCase(letter.charAt(0));
                counts.put(c, counts.getOrDefault(c, 0) + n);
            }
        } else {
            for (char ch : spec.toCharArray()) {
                if (!Character.isLetter(ch)) continue;
                char c = Character.toUpperCase(ch);
                counts.put(c, counts.getOrDefault(c, 0) + 1);
            }
        }
        return counts;
    }

    // Génère machines et executorArgs en garantissant qu'une lettre n'apparaisse que sur une seule machine
    private static GeneratedConfig generateRandomFromSpec(String spec) {
        Map<Character,Integer> global = parseGlobalSpec(spec);
        int distinct = Math.max(1, global.size());
        Random rnd = new Random();
        int maxMachines = Math.min(8, distinct);
        int machineCount = 1 + rnd.nextInt(maxMachines); // 1..maxMachines

        // assigner chaque lettre (toutes ses occurrences) à une seule machine aléatoire
        List<Map<Character,Integer>> perMachine = new ArrayList<>();
        for (int i = 0; i < machineCount; i++) perMachine.add(new HashMap<>());
        List<Character> letters = new ArrayList<>(global.keySet());
        for (char c : letters) {
            int count = global.getOrDefault(c, 0);
            int idx = rnd.nextInt(machineCount);
            Map<Character,Integer> m = perMachine.get(idx);
            m.put(c, m.getOrDefault(c, 0) + count);
        }

        // construire machines au format <port> <ressources>
        List<String[]> machines = new ArrayList<>();
        List<String> machineEndpoints = new ArrayList<>();
        int basePort = 30000 + rnd.nextInt(1000);
        for (int i = 0; i < machineCount; i++) {
            int port = basePort + i;
            Map<Character,Integer> m = perMachine.get(i);
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<Character,Integer> en : m.entrySet()) {
                if (!first) sb.append('/');
                sb.append(en.getValue()).append(en.getKey());
                first = false;
            }
            if (sb.length() == 0) sb.append("0A");
            machines.add(new String[] { Integer.toString(port), sb.toString() });
            machineEndpoints.add("--machine localhost:" + port);
        }

        // Générer réactions réalisables à partir du stock global
        Map<Character,Integer> remaining = new HashMap<>(global);
        List<String> reactions = new ArrayList<>();
        int reactionCount = 1 + rnd.nextInt(Math.min(5, Math.max(1, remaining.size())));
        List<Character> availableLetters = new ArrayList<>();
        for (Map.Entry<Character,Integer> e : remaining.entrySet()) if (e.getValue() > 0) availableLetters.add(e.getKey());

        for (int r = 0; r < reactionCount && !availableLetters.isEmpty(); r++) {
            int maxTypes = Math.min(3, availableLetters.size());
            int reactantTypes = 1 + rnd.nextInt(maxTypes);
            List<Character> chosen = new ArrayList<>();
            while (chosen.size() < reactantTypes) {
                Character c = availableLetters.get(rnd.nextInt(availableLetters.size()));
                if (!chosen.contains(c)) chosen.add(c);
            }
            StringBuilder left = new StringBuilder();
            for (int i = 0; i < chosen.size(); i++) {
                char c = chosen.get(i);
                int avail = remaining.getOrDefault(c, 0);
                if (avail <= 0) continue;
                int use = 1 + rnd.nextInt(avail);
                remaining.put(c, avail - use);
                if (left.length() > 0) left.append('+');
                left.append(use == 1 ? ("" + c) : (use + "" + c));
            }
            if (left.length() == 0) break;
            char product = (char) ('A' + rnd.nextInt(26));
            reactions.add(left.toString() + "->" + product);
            availableLetters.removeIf(l -> remaining.getOrDefault(l, 0) <= 0);
        }

        StringBuilder execSb = new StringBuilder();
        for (String r : reactions) {
            execSb.append("--reaction \"").append(r).append("\" ");
        }
        for (String me : machineEndpoints) execSb.append(me).append(' ');

        return new GeneratedConfig(machines, execSb.toString().trim());
    }

    private static List<String> splitArgs(String raw) {
        List<String> res = new ArrayList<>();
        for (String part : raw.trim().split("\\s+")) {
            if (!part.isEmpty()) {
                res.add(part);
            }
        }
        return res;
    }

    private static void usage() {
        LOGGER.severe("[TEST] Usage : java Test --machine \"<port> <ressources>\" [--machine ...] --executor \"<args executor>\"");
        LOGGER.severe("[TEST] Exemple : java Test --machine \"30000 3A/2B\" --machine \"30001 1A\" --executor \"--reaction 3A+2B->C --machine localhost:30000 --machine localhost:30001\"");
    }
}
