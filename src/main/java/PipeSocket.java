import crypt.CryptFactory;
import crypt.ICrypt;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Executor;

public class PipeSocket implements Runnable {

    private Socket localSocket;
    private Socket remoteSocket;
    private Executor executor;
    private ICrypt crypt;

    PipeSocket(Socket socket, Config config, Executor executor) throws IOException {
        this.localSocket = socket;
        this.remoteSocket = new Socket(config.getRemoteAddress(), config.getRemotePort());
        this.executor = executor;
        this.crypt = CryptFactory.get(config.getMethod(), config.getPassword());

    }

    @Override
    public void run() {
        executor.execute(localSocketHandler());
        executor.execute(remoteSocketHandler());
    }

    private Runnable localSocketHandler() {
        return () -> {
            BufferedInputStream input;
            byte[] inputData = new byte[1024];
            try {
                input = new BufferedInputStream(localSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream(16384);
            int status = 0;
            while (true) {
                try {
                    int readCount = input.read(inputData);
                    if (readCount == -1) {
                        throw new IOException("Local socket closed (Read)!");
                    }
                    switch (status) {
                        case 0:
                            //hello
                            if (inputData[0] != 0x5) throw new IOException("Socks5 protocol head  error");
                            byte[] hello = new byte[]{5, 0};
                            log("send hello to client");
                            send(localSocket, hello);
                            status++;
                            break;
                        case 1:
                            //ack
                            byte[] dist = new byte[readCount - 3];
                            System.arraycopy(inputData, 3, dist, 0, readCount - 3);
                            log("send dist to remote");
                            send(remoteSocket, encrypt(stream, dist, dist.length));
                            byte[] ack = new byte[]{5, 0, 0, 1, 0, 0, 0, 0, 0, 0};
                            log("send ack to client");
                            send(localSocket, ack);
                            status++;
                            break;
                        default:
                            //encrypt
                            log("send encrypted data to remote");
                            send(remoteSocket, encrypt(stream, inputData, readCount));
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
            close();
        };
    }


    private Runnable remoteSocketHandler() {
        return () -> {
            BufferedInputStream input;
            byte[] inputData = new byte[1024];
            try {
                input = new BufferedInputStream(remoteSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream(16384);
            while (true) {
                try {
                    int readCount = input.read(inputData);
                    log("read " + readCount + " bytes from remote");
                    if (readCount == -1) {
                        throw new IOException("Remote socket closed (read)!");
                    }
                    //decrypt
                    send(localSocket, decrypt(stream, inputData, readCount));
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
            close();
        };
    }

    private void send(Socket socket, byte[] data) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(data, 0, data.length);
    }

    private byte[] encrypt(ByteArrayOutputStream stream, byte data[], int length) {
        crypt.encrypt(data, length, stream);
        return stream.toByteArray();
    }

    private byte[] decrypt(ByteArrayOutputStream stream, byte data[], int length) {
        crypt.decrypt(data, length, stream);
        return stream.toByteArray();
    }


    private void close() {
        try {
            if (!remoteSocket.isClosed()) {
                remoteSocket.close();
            }
            if (!localSocket.isClosed()) {
                localSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void log(String msg) {
        System.out.println(msg);
    }

}
