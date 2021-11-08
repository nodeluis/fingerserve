package com.fingerprint.sisrapsur;

import org.json.JSONObject;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

@ServerEndpoint(value = "/finger",configurator = Configurator.class)
public class FingerServerEndPoint {

    public static final String REGISTER_STATE = "registering";
    public static final String VERIFY_STATE = "verifying";
    public static final String NONE_STATE = "none";

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private FingerPrint fingerPrint;
    private Set<Session> clients;
    private String state;
    private String fingerprintData;


    public FingerServerEndPoint(){
        clients = new HashSet<Session>();
        state = FingerServerEndPoint.NONE_STATE;
        fingerprintData = "";
    }

    @OnOpen
    public void onOpen(Session session){
        logger.info("Connected with" + session.getId());
        clients.add(session);
    }

    @OnMessage
    public String onMessage(String message, Session session){
        JSONObject request = new JSONObject(message);
        JSONObject response = new JSONObject();
        response.put("type", "received");
        String type = request.getString("type");
        switch(type){
            case "quit":
                try{
                    session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Finalizado correctamente"));
                }catch(IOException e){
                    throw new RuntimeException(e);
                }
                break;
            case "prepare-register":
                this.setState(FingerServerEndPoint.REGISTER_STATE);
                fingerPrint = new FingerPrint(this);
                fingerPrint.start();
                break;
            case "prepare-verify":
                this.setState(FingerServerEndPoint.VERIFY_STATE);
                this.setFingerprintData(request.getString("data"));
                fingerPrint = new FingerPrint(this);
                fingerPrint.start();
                break;
        }
        return response.toString();
    }

    @OnClose
    public void OnClose(Session session, CloseReason closeReason){
        logger.info(String.format("Session %s closed because of %s", session.getId(), closeReason));
        clients.remove(session);
        if(fingerPrint != null){
            fingerPrint.stop();
        }
    }

    public void sendResponse( JSONObject response ){
        for(Session client: clients){
            client.getAsyncRemote().sendText(response.toString());
        }
    }


    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getFingerprintData() {
        return fingerprintData;
    }

    public void setFingerprintData(String fingerprintData) {
        this.fingerprintData = fingerprintData;
    }
}
