import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Test {
    private static final Logger LOGGER = Logger.getLogger(Test.class.getName());

    public static void main(String[] args) {
        List<String[]> machines = new ArrayList<>();
        String executorArgs = null;

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
            } else {
                LOGGER.warning("[TEST] Argument inconnu ignoré : " + arg);
            }
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
