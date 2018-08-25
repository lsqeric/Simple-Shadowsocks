import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LocalServer {

    public static void main(String[] args) throws IOException {
        Config config = Config.getInstance();
        ServerSocket serverSocket = new ServerSocket(config.getLocalPort());
        Executor executor = Executors.newCachedThreadPool();
        while(true) {
            Socket socket = serverSocket.accept();
            PipeSocket pipeSocket = new PipeSocket(socket,config,executor);
            executor.execute(pipeSocket);
        }
    }
}
