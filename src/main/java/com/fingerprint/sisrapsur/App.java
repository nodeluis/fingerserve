package com.fingerprint.sisrapsur;

import org.glassfish.tyrus.server.Server;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        runServer();
    }

    private static void runServer(){

        Server server = new Server("localhost", 9090, "/finger", FingerServerEndPoint.class);

        try{
            server.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Presione una tecla para salir del servidor");
            reader.readLine();
        }catch(Exception e){
            throw new RuntimeException(e);
        }finally{
            server.stop();
        }
    }
}
