import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class NioLocalServer {
    private static Logger logger = Logger.getLogger(NioPipeSocket.class.getName());
    public static void main(String[] args)  {
        Config config = Config.getInstance();
        ServerSocketChannel serverSocketChannel;
        Selector selector;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(config.getLocalPort()));
            selector = Selector.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        }catch (IOException e){
            e.printStackTrace();
            return;
        }

        while(true){
            try {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> itr = selectionKeys.iterator();
                    while (itr.hasNext()) {
                        SelectionKey key = itr.next();
                        if (key.isValid()&&key.isAcceptable()) {
                            logger.info("Server socket accept");
                            SocketChannel localSocketChannel = ((ServerSocketChannel) key.channel()).accept();
                            SocketChannel remoteSocketChannel = SocketChannel.open();
                            remoteSocketChannel.connect(new InetSocketAddress(config.getRemoteAddress(), config.getRemotePort()));

                            localSocketChannel.configureBlocking(false);
                            remoteSocketChannel.configureBlocking(false);
                            SelectionKey localKey = localSocketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                            SelectionKey remoteKey = remoteSocketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                            NioPipeSocket pipeSocket = new NioPipeSocket(localSocketChannel, remoteSocketChannel, config);

                            localKey.attach(pipeSocket);
                            remoteKey.attach(pipeSocket);
                        }
                        if(key.isValid()&&key.isWritable()){
                            NioPipeSocket pipeSocket = (NioPipeSocket) key.attachment();
                            //executor.execute(() -> pipeSocket.write(key));
                            pipeSocket.write(key);
                        }
                        if(key.isValid()&&key.isReadable()){
                            NioPipeSocket pipeSocket = (NioPipeSocket) key.attachment();
                            //executor.execute(() -> pipeSocket.read(key));
                            pipeSocket.read(key);
                        }
                        itr.remove();
                }
            }catch (IOException e){
                e.printStackTrace();
                break;
            }
        }

        try {
            serverSocketChannel.close();
            selector.close();
        }catch (IOException e){
            e.printStackTrace();
        }

    }
}
