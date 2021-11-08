package com.fingerprint.sisrapsur;

import javax.websocket.server.ServerEndpointConfig;
import java.util.logging.Logger;

public class Configurator extends ServerEndpointConfig.Configurator {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private String url1="http://localhost:8080";

    @Override
    public boolean checkOrigin(String originHeaderValue) {

        logger.info("Url permitida "+url1);

        if(originHeaderValue.equals(url1))
            return  true;
        else
            return false;
    }

}
