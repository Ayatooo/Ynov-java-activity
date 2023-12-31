package fr.ayato.activity;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@Slf4j
public class Connection {

    public static MongoCollection<Document> client(String databaseName, String collectionName) {
        try {
            Dotenv dotenv = Dotenv.configure().load();
            String connectionString = dotenv.get("MONGODB_URI");
            assert connectionString != null;
            MongoClient client = MongoClients.create(connectionString);
            return client.getDatabase(databaseName).getCollection(collectionName);
        } catch (Exception e) {
            log.error("Error while connecting to MongoDB", e);
            throw new RuntimeException(e);
        }
    }
}
