package org.hchiu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Acceptor;
import quickfix.DefaultMessageFactory;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class DynamicSessionMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSessionMain.class);
    private static final String CONFIG_TEMPLATE = "acceptor_base.config";
    private static final ClassLoader MAIN_LOADER = DynamicSessionMain.class.getClassLoader();
    private static final Map<String, Acceptor> targetCompIdAcceptors = new HashMap<>();

    public static void main(String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        boolean isRun = true;
        LOGGER.info("Use command \"add abc\" to add a new client session abc.");
        LOGGER.info("Use quit to exit.");
        while (isRun) {
            String value = in.readLine();
            if (value != null) {
                if (value.startsWith("add")) {
                    String client = value.split(" ")[1];
                    targetCompIdAcceptors.computeIfAbsent(client, DynamicSessionMain::addAcceptor);
                } else if (value.startsWith("deactivate")) {
                    String client = value.split(" ")[1];
                    consumeSession(client, (Session session) -> {
                        LOGGER.info("deactivating session {}", client);
                        session.logout("admin logout");
                    });
                } else if (value.startsWith("activate")) {
                    String client = value.split(" ")[1];
                    consumeSession(client, (Session session) -> {
                        LOGGER.info("activating session {}", client);
                        session.logon();
                    });
                } else if (value.startsWith("quit")) {
                    isRun = false;
                }
            }
        }
        targetCompIdAcceptors.values().forEach(Acceptor::stop);
        System.exit(0);
    }

    private static void consumeSession(String client, Consumer<Session> consumer) {
        if (targetCompIdAcceptors.containsKey(client)) {
            targetCompIdAcceptors.get(client).getSessions()
                    .forEach(sessionID -> consumer.accept(Session.lookupSession(sessionID)));
        } else {
            LOGGER.warn("{} session not found", client);
        }
    }

    private static Acceptor addAcceptor(String client) throws RuntimeException {
        try {
            int n = targetCompIdAcceptors.size();
            String port = n < 10 ? "2000" + n : "200" + n;

            AcceptorApp application = new AcceptorApp();
            SessionSettings settings = new SessionSettings(MAIN_LOADER.getResourceAsStream(CONFIG_TEMPLATE));
            addSession(settings, client, port);
            MessageStoreFactory storeFactory = new FileStoreFactory(settings);
            LogFactory logFactory = new FileLogFactory(settings);
            MessageFactory messageFactory = new DefaultMessageFactory();
            Acceptor acceptor = new SocketAcceptor(application, storeFactory, settings, logFactory, messageFactory);

            acceptor.start();
            synchronized (DynamicSessionMain.class) {
                Thread.sleep(1000);  //wait for acceptor starting asynchronously
            }
            LOGGER.info("starting acceptor for {} on port {}", acceptor.getSessions().get(0).getTargetCompID(), port);

            return acceptor;
        } catch (Exception exp) {
            throw new RuntimeException("failed to add acceptor", exp);
        }
    }

    private static void addSession(SessionSettings settings, String client, String port) {
        var defaultProp = settings.getDefaultProperties();
        var sessionId = new SessionID(
                defaultProp.getProperty("BeginString"),
                defaultProp.getProperty("SenderCompID"),
                client);
        settings.setString(sessionId, "SocketAcceptPort", port); // triggers internal getOrCreateSession
    }
}
