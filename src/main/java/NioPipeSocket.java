import crypt.CryptFactory;
import crypt.ICrypt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.logging.Logger;

public class NioPipeSocket{
    private static Logger logger = Logger.getLogger(NioPipeSocket.class.getName());
    private SocketChannel localSocketChannel;
    private SocketChannel remoteSocketChannel;
    private ByteBuffer localBuffer;
    private ByteBuffer remoteBuffer;
    private ICrypt crypt;
    private ByteArrayOutputStream stream;
    private List<byte[]> send2Local;
    private List<byte[]> send2Remote;
    private int status;

    public NioPipeSocket(SocketChannel local, SocketChannel remote, Config config){
        this.localSocketChannel = local;
        this.remoteSocketChannel = remote;
        this.localBuffer = ByteBuffer.allocate(1024);
        this.remoteBuffer = ByteBuffer.allocate(1024);
        this.crypt = CryptFactory.get(config.getMethod(),config.getPassword());
        this.stream = new ByteArrayOutputStream(1024);
        this.status = 0;
        this.send2Local = new ArrayList<>();
        this.send2Remote = new ArrayList<>();
        logger.info("Nio Pipe Socket created");

    }

    public void write(SelectionKey selectionKey){
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        try {
            if (socketChannel == localSocketChannel) {
                // write local
                send(send2Local, socketChannel, localBuffer);
                send2Local.clear();
            } else if (socketChannel == remoteSocketChannel) {
                // write remote
                send(send2Remote, socketChannel, remoteBuffer);
                send2Remote.clear();
            } else {
                throw new IOException("Wrong socket channel");
            }
        }catch (IOException e){
            e.printStackTrace();
            close();
        }
    }

    public void read(SelectionKey selectionKey){
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        try{
            if(socketChannel == localSocketChannel){
                localBuffer.clear();
                int bytesRead = socketChannel.read(localBuffer);
                while (bytesRead > 0) {
                    localBuffer.flip();
                    byte[] data = new byte[bytesRead];
                    System.arraycopy(localBuffer.array(), 0, data, 0, bytesRead);
                    switch (status) {
                        case 0:
                            //hello
                            if (data[0] != 0x5) throw new IOException("Socks5 protocol head  error");
                            byte[] hello = new byte[]{5, 0};
                            send2Local.add(hello);
                            status++;
                            break;
                        case 1:
                            //ack
                            byte[] dist = new byte[bytesRead - 3];
                            System.arraycopy(data, 3, dist, 0, bytesRead - 3);
                            send2Remote.add(encrypt(dist, dist.length));
                            byte[] ack = new byte[]{5, 0, 0, 1, 0, 0, 0, 0, 0, 0};
                            send2Local.add(ack);
                            status++;
                            break;
                        default:
                            send2Remote.add(encrypt(data, data.length));
                            break;
                    }
                    localBuffer.clear();
                    bytesRead = socketChannel.read(localBuffer);
                }
                if (bytesRead == -1) {
                    throw new IOException("Local socket closed (Read)");
                }

            }else if(socketChannel == remoteSocketChannel){
                remoteBuffer.clear();
                int bytesRead = socketChannel.read(remoteBuffer);
                while (bytesRead > 0) {
                    remoteBuffer.flip();
                    byte[] data = new byte[bytesRead];
                    System.arraycopy(remoteBuffer.array(), 0, data, 0, bytesRead);
                    send2Local.add(decrypt(data, data.length));
                    remoteBuffer.clear();
                    bytesRead = socketChannel.read(remoteBuffer);
                }
                if (bytesRead == -1) {
                    throw new IOException("remote socket closed (Read)");
                }
            }else{
                throw new IOException("Wrong socket channel");
            }

        }catch (IOException e){
            e.printStackTrace();
            close();
        }

    }

    private void send(List<byte[]> dataList, SocketChannel socketChannel, ByteBuffer buffer) throws IOException {
        for(byte[] data:dataList){
            buffer.clear();
            buffer.put(data);
            buffer.flip();
            socketChannel.write(buffer);
        }
    }

    private byte[] encrypt(byte data[], int length) {
        crypt.encrypt(data, length, stream);
        return stream.toByteArray();
    }

    private byte[] decrypt(byte data[], int length) {
        crypt.decrypt(data, length, stream);
        return stream.toByteArray();
    }

    private void close(){
        try {
            this.remoteSocketChannel.close();
            this.localSocketChannel.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
