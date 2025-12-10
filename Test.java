import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {
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
                System.err.println("Argument inconnu ignoré : " + arg);
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
                    System.err.println("Format machine invalide. Attendu : \"<port> <ressources>\"");
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
            System.out.println("Executor terminé avec le code : " + code);
        } catch (IOException e) {
            System.err.println("Echec lors du lancement des processus : " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interruption lors de l'attente de l'executor : " + e.getMessage());
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
        System.err.println("Usage : java Test --machine \"<port> <ressources>\" [--machine ...] --executor \"<args executor>\"");
        System.err.println("Exemple : java Test --machine \"30000 3A/2B\" --machine \"30001 1A\" --executor \"--reaction 3A+2B->C --machine localhost:30000 --machine localhost:30001\"");
    }
}
