package com.fingerprint.sisrapsur;

import com.mongodb.DBObject;
import com.mongodb.client.*;
import org.bson.Document;
import org.glassfish.tyrus.server.Server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

/**
 * Hello world!
 *
 */
public class App 
{

    public static void main( String[] args )
    {

        /*MongoClient mongoClient= MongoClients.create();
        MongoDatabase db = mongoClient.getDatabase("tickeo");

        // Gets the persons collections from the database.
        MongoCollection<Document> collection = db.getCollection("users");

        // Gets a single document / object from this collection.
        MongoCursor<Document> cursor = collection.find().iterator();

        // Prints out the document.
        while (cursor.hasNext()) {
            System.out.println("collection is " +cursor.next().toJson() );
        }*/

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
