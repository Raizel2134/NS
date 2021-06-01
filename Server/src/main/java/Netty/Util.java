package Netty;

import Message.FileListMessage;
import io.netty.channel.Channel;

import java.nio.file.Paths;

public class Util {

    public static void sendFileList(Channel channel, String login) {
        FileListMessage fm = new FileListMessage(Paths.get(getUserPath(login)));
        channel.writeAndFlush(fm);
    }

    public static String getUserPath(String login) {
        return "cloud_repository/" + login + "/";
    }
}
