import Netty.ServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import Аuthorization.AuthService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class Server {
    private static final int PORT = 8189;
    private final Path ROOT = Paths.get("cloud_repository");
    private static final int MAX_OBJ_SIZE = 1024 * 1024 * 100;
    private AuthService authService;

    private void run() {

        if (Files.exists(ROOT, LinkOption.NOFOLLOW_LINKS)) {
            Logger.getGlobal().info("ROOT DIRECTORY " + ROOT);
        } else {
            try {
                Files.createDirectory(ROOT);
                Logger.getGlobal().warning("ATTENTION! --- NEW ROOT DIRECTORY CREATED " + ROOT);
            } catch (IOException e) {
                Logger.getGlobal().severe("FATAL ERROR! FAILED TO CREATE ROOT DIRECTORY. \nSERVER CLOSED");
                return;
            }
        }

        try {
            authService = new AuthService("jdbc:mysql://localhost:3306/storage?serverTimezone=UTC&useSSL=false");
            authService.start();
            Logger.getGlobal().info("AUTHORISATION SERVICE STARTED");
        } catch (Exception e) {
            e.printStackTrace();
            Logger.getGlobal().severe("FATAL ERROR! FAILED TO START AUTHORISATION SERVICE. \nSERVER CLOSED");
            return;
        }

        EventLoopGroup mainGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(mainGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline().addLast(
                                    new ObjectDecoder(MAX_OBJ_SIZE, ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new ServerHandler(ROOT.toAbsolutePath(), authService)
                            );
                        }
                    });
            ChannelFuture future = b.bind(PORT).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mainGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            authService.stop();
        }
    }

    public static void main(String[] args) {
        new Server().run();
    }
}
