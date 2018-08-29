package com.github.dapeng.socket.server;

import com.corundumstudio.socketio.SocketIOServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

/**
 * Created by duwupeng on 16/10/18.
 */
public class CmdExecutor implements Runnable {
    private static Logger LOGGER = LoggerFactory.getLogger(CmdExecutor.class);
    public BlockingQueue queue;
    SocketIOServer server;

    public CmdExecutor(BlockingQueue queue, SocketIOServer server) {
        this.queue = queue;
        this.server = server;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String event = (String) queue.take();
                LOGGER.info("Consumed: " + event);

                server.getRoomOperations("web").sendEvent(event + "Event", "started");

                BuildServerShellInvoker.executeShell(server, event);

                server.getRoomOperations("web").sendEvent(event + "Event", "end");
            } catch (Exception ex) {
                LOGGER.error("::::::executeShell error");
            }
        }
    }
}
