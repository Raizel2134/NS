package NIOServer;

import java.io.*;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NIOServer implements Runnable{
    private static int PORT = 8189;
    private static int BUFF_SIZE = 256;
    private static int clientCount = 0;
    private String dir;
    private String serverPath = "C:\\Users\\edyso\\Desktop\\NS\\Server\\src\\main\\resources\\serverPath";
    private File userDir;
    private DataInputStream in;
    private DataOutputStream out;

    private ServerSocketChannel ssc;
    private Selector selector;
    private ByteBuffer buff = ByteBuffer.allocate(BUFF_SIZE);

    private NIOServer() throws IOException {
        ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress(PORT));
        ssc.configureBlocking(false);
        selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void run(){
        try {
            System.out.println("Server started on port: " + PORT);
            Iterator<SelectionKey> iterator;
            SelectionKey key;
            while (ssc.isOpen()){
                int eventsCount = selector.select();
                System.out.println("Selected " + eventsCount + " event.");
                iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()){
                    key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()){
                        handleAccess(key);
                    }
                    if(key.isReadable()){
                        handleRead(key);
                    }
                }
            }
        } catch (Exception exc){
            exc.printStackTrace();
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder msg = new StringBuilder();
        int read;
        buff.clear();
        while ((read = channel.read(buff)) > 0){
            buff.flip();
            byte [] bytes = new byte[buff.limit() - 1];
            buff.get(bytes);
            msg.append(new String(bytes));
            buff.clear();
        }
        if (read < 0){
            System.out.println(key.attachment() + " leave!");
            channel.close();
        } else {
            System.out.println(key.attachment() + ": " + msg);
            String message = key.attachment() + ": " + msg;
            for (SelectionKey send : key.selector().keys()){
                if (send.channel() instanceof SocketChannel && send.isReadable()){
                    if (msg.toString().startsWith("/send")){
                        String fileName = "lol.txt";
                        File file = new File(dir + "\\" + fileName);
                        RandomAccessFile raf = new RandomAccessFile(dir + "\\" + fileName, "rw");
                        if(!String.valueOf(msg).equals("/send")){
                            raf.writeUTF(String.valueOf(msg));
                        }
                    }
                    ((SocketChannel) send.channel()).write(ByteBuffer.wrap(message.getBytes()));
                }
            }
        }
    }

    private void handleAccess(SelectionKey key) throws IOException {
        SocketChannel channel = ((ServerSocketChannel)key.channel()).accept();
        clientCount++;
        String userName = "user#" + clientCount;
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ, userName);
        channel.write(ByteBuffer.wrap(("Hello " + userName + "!\n").getBytes()));
        System.out.println("Client " + userName + " connected from ip: " + channel.getLocalAddress());
        userDir = new File(serverPath + "\\" + userName);
        if (!userDir.exists()) {
            boolean created = userDir.mkdir();
        }
        dir = userDir.getAbsolutePath();
    }

    public static void main(String[] args) throws IOException {
        new Thread(new NIOServer()).start();
    }

}
