package main;

/** Class: MongoConnect
 * Description: Handles Inserting Documents into Mongo Database
 */

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;

public class MongoConnect {
	
	private MongoCollection collection; 
	MongoClient client;
	
	/** Constructor */
	public MongoConnect() {
		//Create Client On Localhost and default port TODO Configurable Server
		client = new MongoClient();
		collection = client.getDatabase(Rules.getDatabase()).getCollection(Rules.getCollection());
	}
	
	/** onExit */
	public void onExit() {
		client.close();
	}
	
	/** insert */
	public void insert (Document doc) {
		collection.insertOne(doc);
	}
}
