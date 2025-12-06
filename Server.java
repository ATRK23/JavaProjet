import java.util.concurrent.*;
import java.io.*;
import java.net.*;

public class Server{
    private ExecutorService pool;
    private int port;
    private int poolSize;
    private boolean isFinished;
    private ServerSocket server;

    public Server(int port, int poolSize){
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
                this.pool.execute(new Slave(server.accept()));
                System.out.println("Connection  [OK] ");
            }

        } catch (IOException E) {
            E.printStackTrace();
        }
    }
}