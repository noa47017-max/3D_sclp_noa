package com.noa.packer.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoConnection {
    private static MongoClient client;
    private static final String URI = 
        "mongodb+srv://noa47017_db_user:0hKTRe8ji3mYHzRh@noadb.k3kbfnv.mongodb.net/?appName=NoaDB";

    public static MongoDatabase connect() {
        client = MongoClients.create(URI);
        System.out.println("✅ Connected to Atlas!");
        return client.getDatabase("3D_SCLP");
    }

    public static void disconnect() {
        if (client != null) client.close();
    }
}
