import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Main
{
    static void sendBroadcast() throws IOException, InterruptedException {
        //final String MCAST_ADDR = "FF7E:230::1234";
        final String MCAST_ADDR = "255.255.255.255";
        final InetAddress GROUP = InetAddress.getByName(MCAST_ADDR);

        DatagramSocket socket = new DatagramSocket(4445);
        socket.setBroadcast(true);
        socket.setSoTimeout(60000);

        final byte response[] = "LestoBackupper0".getBytes();
        final byte request[] = "LestoBackupper?".getBytes();
        final byte requestBuff[] = new byte[request.length];

        DatagramPacket receive = new DatagramPacket(requestBuff, requestBuff.length);
        while (!Thread.currentThread().isInterrupted()) {
            boolean sendBroadcast;
            try {
                requestBuff[0] = 0; // make sure we don't loop over old messages
                socket.receive(receive);
                sendBroadcast = Arrays.equals(receive.getData(), request);
            } catch (SocketTimeoutException e) {
                sendBroadcast = true;
            }
            if (sendBroadcast) {
                System.out.println("BROADCAST!");
                DatagramPacket packet = new DatagramPacket(response, response.length, GROUP, 4446);
                socket.send(packet);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Thread(() -> {
            try {
                sendBroadcast();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        ServerSocket serverSocket = new ServerSocket(9999);
        System.out.println("server running");

        final File tempFolder = new File("/tmp/lestobackUpper");
        tempFolder.mkdirs();
        while (!Thread.currentThread().isInterrupted()) {
            try (
                Socket clientSocket = serverSocket.accept();
            ) {
                receiveFile(clientSocket, tempFolder);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    private static void receiveFile(Socket clientSocket, final File tempFolder) throws URISyntaxException {
        File tempFileName;
        System.out.println("temp file created");
        try (
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
        ) {
            tempFileName = File.createTempFile("a.lb-", ".temp.jpg", tempFolder);
            System.out.println("got new connection");
            String id;
            {
                int idLen = in.readInt();
                System.out.println("got idLen "+idLen);
                byte buf[] = new byte[idLen];
                in.readFully(buf);
                id = new String(buf);
                System.out.println("id: " + id);
            }

            String basePath;
            {
                int basePathLen = in.readInt();
                System.out.println("got basePathLen "+basePathLen);
                byte buf[] = new byte[basePathLen];
                in.readFully(buf);
                basePath = new String(buf);
                System.out.println("basePath: " + basePath);
            }

            String path;
            {
                int pathLen = in.readInt();
                System.out.println("got pathLen "+pathLen);
                byte buf[] = new byte[pathLen];
                in.readFully(buf);
                path = new String(buf);
                System.out.println("path: " + path);
            }

            long fileLen;
            byte hash[];
            File outPath;
            {
                fileLen = in.readLong();
                System.out.println("got fileLen " + fileLen);

                hash = new byte[32]; //sha 256
                in.readFully(hash);
                System.out.println("got hash " + Arrays.toString(hash));

                //TODO: replace with a map based on basePath
                File baseOutPath = new File("/tmp/tests/"+id);
                baseOutPath.mkdirs();

                if (!path.startsWith(basePath)){
                    System.out.println("cant understand relative path between " + path + " and " + basePath);
                    return;
                }

                String relative = path.substring(basePath.length());
                String fileRelativePath = URLDecoder.decode(relative, "utf-8");
                outPath = new File(baseOutPath, fileRelativePath);
                System.out.println("relative is: "+relative+ " uri: "+fileRelativePath + " final path is " + outPath);
                File parent = outPath.getParentFile();
                if (parent != null){
                    // make sure the file has the folders required
                    parent.mkdirs();
                }
            }

            // force upload request
            out.writeBoolean(true);

            {
                try (FileOutputStream f = new FileOutputStream(tempFileName)) {
                    byte buf[] = new byte[8192];
                    long start = System.currentTimeMillis();
                    long missing = fileLen;
                    long lastMissing = missing;
                    while (missing >= buf.length) {
                        in.readFully(buf);
                        f.write(buf);
                        missing -= buf.length;
                        if (System.currentTimeMillis() - start >= 1000){
                            System.out.println("speed: " + (lastMissing - missing));
                            lastMissing = missing;
                            start = System.currentTimeMillis();
                        }
                    }
                    if (missing > 0) {
                        //cast to int is safe as buf.length should be int
                        assert (missing < Integer.MAX_VALUE);
                        byte remaining[] = new byte[(int) missing];
                        in.readFully(remaining);
                        f.write(remaining);
                    }
                }catch(Exception e){
                    e.printStackTrace();
                    throw e;
                }
                System.out.println("saved as: " + tempFileName);
            }

            {
                byte[] buffer = new byte[8192];
                int count;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(tempFileName));
                while ((count = bis.read(buffer)) > 0) {
                    digest.update(buffer, 0, count);
                }
                bis.close();

                byte[] calc_hash = digest.digest();
                System.out.println("hashed as: " + Arrays.toString(calc_hash) + " expected: " + calc_hash);
                boolean hashAreEquals = Arrays.compare(calc_hash, hash) == 0;
                out.writeBoolean(hashAreEquals);
                if (hashAreEquals){
                    boolean result = tempFileName.renameTo(outPath);
                    out.writeBoolean(result);
                }else{
                    //invalid transfer!
                    tempFileName.delete();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } finally {
//                if (tempFileName != null)
//                    tempFileName.delete();
        }
    }
}

/*
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main
{
    enum Step{
        ID_LEN, ID,
        PATH_BASE_LEN, PATH_BASE,
        PATH_LEN, PATH,
        FILE_LEN, FILE,
        SEND_CRC,
        WAITING_CONFIRMATION
    };

    Step status = Step.ID;

    class ReceivedData{
        String id = null;
        String basePath = null;
        String path = null;
        File tempFile = null;
        FileChannel channel = null;
    }

    public Main()
    {
        try {
            // Create an AsynchronousServerSocketChannel that will listen on port 5000
            final AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel
                    .open()
                    .bind(new InetSocketAddress(5000));

            // Listen for a new request
            listener.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>()
            {
                @Override
                public void completed(AsynchronousSocketChannel ch, Void att)
                {
                    // Accept the next connection
                    listener.accept(null, this);

                    // Allocate a byte buffer (4K) to read from the client
                    ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
                    ReceivedData received = new ReceivedData();
                    try {

                        int bytesRead = 0;
                        boolean running = true;
                        long bytesToParse = 8;

                        while (running) {

                            bytesRead = ch.read(byteBuffer).get(20, TimeUnit.SECONDS);
                            System.out.println("bytes read: " + bytesRead);

                            byteBuffer.flip();
                            while (byteBuffer.position() >= bytesToParse) {

                                // Make the buffer ready to read
                                switch(status){
                                    case ID_LEN -> {
                                        status = Step.ID;
                                        bytesToParse = byteBuffer.getLong();
                                    }
                                    case ID -> {
                                        byte support_buff[] = new byte[(int)bytesToParse];
                                        byteBuffer.get(support_buff, 0, support_buff.length);

                                        received.id = new String(support_buff);

                                        status = Step.PATH_BASE_LEN;
                                        bytesToParse = 8;
                                    }
                                    case PATH_BASE_LEN -> {
                                        status = Step.PATH_BASE;
                                        bytesToParse = byteBuffer.getLong();
                                    }
                                    case PATH_BASE -> {
                                        byte support_buff[] = new byte[(int)bytesToParse];
                                        byteBuffer.get(support_buff, 0, support_buff.length);

                                        received.basePath = new String(support_buff);

                                        status = Step.PATH_LEN;
                                        bytesToParse = 8;
                                    }
                                    case PATH_LEN -> {
                                        status = Step.PATH;
                                        bytesToParse = byteBuffer.getLong();
                                    }
                                    case PATH -> {
                                        byte support_buff[] = new byte[(int)bytesToParse];
                                        byteBuffer.get(support_buff, 0, support_buff.length);

                                        received.path = new String(support_buff);
                                        received.tempFile = File.createTempFile(".lbs", ".temp");
                                        received.channel = new FileOutputStream(received.tempFile, true).getChannel();

                                        //save here the path, calculate the final file path, open the file for write
                                        status = Step.FILE_LEN;
                                        bytesToParse = 8;
                                    }
                                    case FILE_LEN -> {
                                        status = Step.FILE;
                                        bytesToParse = byteBuffer.getLong();
                                    }
                                    case FILE -> {

                                        received.channel.write(byteBuffer, bytesToParse);

                                        status = Step.WAITING_CONFIRMATION;
                                        bytesToParse = 8;
                                    }
                                    case SEND_CRC -> {
                                        //close file, read crc and write it back
                                        status = Step.WAITING_CONFIRMATION;
                                        bytesToParse = 1;
                                    }
                                    case WAITING_CONFIRMATION -> {
                                        //close file, if confirmation is OK then rename the file
                                        running = false;
                                    }
                                }
                            }

                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        // The user exceeded the timeout, so close the connection
                        System.out.println("Connection timed out, closing connection");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally{
                        if (received.tempFile != null) {
                            received.tempFile.delete();
                        }

                    }

                    System.out.println("End of conversation");
                    try {
                        // Close the connection if we need to
                        if (ch.isOpen()) {
                            ch.close();
                        }
                    } catch (IOException e1)
                    {
                        e1.printStackTrace();
                    }
                }

                @Override
                public void failed(Throwable exc, Void att)
                {
                    ///...
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        Main server = new Main();
        try {
            Thread.sleep(60000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
*/