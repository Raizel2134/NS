package NIOClient;


import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class NIOClient {


    public static void main(String[] args) throws IOException {
        InetAddress addr = InetAddress.getByName(null);
        System.out.println("addr = " + addr);
        Socket socket = new Socket(addr, 8189);
        try {
            System.out.println("socket = " + socket);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket
                    .getInputStream()));
            PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())), true);
            Scanner inp = new Scanner(System.in);
            String str = "";
            while (!str.equals("exit")){
                if(str.equals("/send")){
                    RandomAccessFile raf = new RandomAccessFile("C:\\Users\\edyso\\Desktop\\NS\\Client\\src\\main\\resources\\clientPath\\lol.txt", "rw");
                    out.println(str);
                    byte[] buffer = new byte[(int) raf.length()];
                    for (int i = 0; raf.length() < i; i++){
                        buffer[i] = raf.readByte();
                    }
                    out.println(buffer);
                } else {
                    out.println(str);
                }
                str = inp.nextLine();
            }
        }
        finally {
            System.out.println("closing...");
            socket.close();
        }
    }
}
