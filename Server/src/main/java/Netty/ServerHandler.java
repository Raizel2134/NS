package Netty;

import Message.AuthMessage;
import Message.CommandMessage;
import Message.DataTransferMessage;
import Message.ResultMessage;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import Аuthorization.AuthService;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.logging.Logger;

public class ServerHandler extends ChannelInboundHandlerAdapter {
    private String login;
    private final Path SERVER_DIRECTORY;
    private Path currentDirectory;
    private final AuthService AUTH_SERVICE;
    private boolean isAuth;

    public ServerHandler(Path serverDirectory, AuthService AUTH_SERVICE) {
        this.SERVER_DIRECTORY = serverDirectory;
        this.AUTH_SERVICE = AUTH_SERVICE;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Logger.getGlobal().info("CLIENT CONNECTED: " + InetAddress.getLocalHost().getHostName());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null) return;
            if (!isAuth) {
                if (msg instanceof AuthMessage) {
                    String login = ((AuthMessage) msg).getLogin();
                    String password = ((AuthMessage) msg).getPassword();
                    Path path = Paths.get("cloud_repository", login);
                    if (AUTH_SERVICE.isLoginAccepted(login, password)) {
                        if (Files.isDirectory(SERVER_DIRECTORY.resolve(login), LinkOption.NOFOLLOW_LINKS)) {
                            currentDirectory = SERVER_DIRECTORY.resolve(login);
                        } else {
                            Files.createDirectory(path);
                        }

                        this.login = login;
                        isAuth = true;

                        ChannelFuture channelFuture = ctx.writeAndFlush(ResultMessage.Result.OK);
                        channelFuture.await();

                        Util.sendFileList(ctx.channel(), login);
                    } else {
                        ctx.write(new ResultMessage(ResultMessage.Result.FAILED));
                        ctx.flush();
                    }
                }
            } else {
                if (msg instanceof DataTransferMessage) {
                    Logger.getGlobal().info("Incoming File");
                    DataTransferMessage data = (DataTransferMessage) msg;
                    Path path = Paths.get(SERVER_DIRECTORY + "/" + login + "/" + data.getFileName());
                    try {
                        if (Files.exists(path)) {
                            Files.write(path, data.getData(), StandardOpenOption.TRUNCATE_EXISTING);
                        } else {
                            Files.write(path, data.getData(), StandardOpenOption.CREATE);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Util.sendFileList(ctx.channel(), login);
                }
                if (msg instanceof CommandMessage) {
                    Logger.getGlobal().info("Processing command: " + ((CommandMessage) msg).getCommand());
                    switch (((CommandMessage) msg).getCommand()) {
                        case DELETE:
                            Path pathFile = Paths.get(((CommandMessage) msg).getAddition());
                            Path path = Paths.get(SERVER_DIRECTORY + "/" + login + "/");
                            System.out.println(Paths.get(path.toString() + "/" +  pathFile.getFileName()));
                            Files.delete(Paths.get(path.toString() + "/" + pathFile.getFileName()));
                            Util.sendFileList(ctx.channel(), login);
                            break;
                        case LIST_FILES:
                            Util.sendFileList(ctx.channel(), login);
                            break;
                        case DOWNLOAD:
                            ChannelFuture channelFuture = ctx.writeAndFlush(new DataTransferMessage(Paths.get(((CommandMessage) msg).getAdditionFile().getPath())));
                            break;
                        case RENAME:
                            CommandMessage renameMsg = (CommandMessage) msg;
                            File fileForRenaming = renameMsg.getAdditionFile();
                            String newName = renameMsg.getAddition();
                            String expansion = null;
                            try {
                                expansion = fileForRenaming.getName().split("\\.")[1];
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            try {
                                Path source = Paths.get(fileForRenaming.getPath());
                                if (expansion == null) {
                                    Files.move(source, source.resolveSibling(newName));
                                } else {
                                    if (newName.contains(".")) {
                                        Files.move(source, source.resolveSibling(newName));
                                    } else {
                                        Files.move(source, source.resolveSibling(newName + "." + expansion));
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Util.sendFileList(ctx.channel(), login);
                            break;
                    }
                } else {
                    Logger.getGlobal().warning("Server received wrong object from " + login);
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        isAuth = false;
        ctx.flush();
        ctx.close();
    }
}
