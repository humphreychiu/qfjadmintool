package org.hchiu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;

public class AcceptorApp implements Application {

    private Logger logger = LoggerFactory.getLogger(AcceptorApp.class);
    private boolean isActive = true;

    private SessionID sess;

    public void activate() {
        Session.lookupSession(sess).logon();
    }

    @Override
    public void onCreate(SessionID sessionID) {
        sess = sessionID;
    }

    @Override
    public void onLogon(SessionID sessionID) {
        logger.info("logon {}", sessionID);
    }

    @Override
    public void onLogout(SessionID sessionID) {
        logger.info("logout {}", sessionID);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {

    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
    }

    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {

    }

    @Override
    public void fromApp(Message message, SessionID sessionID)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
    }
}
