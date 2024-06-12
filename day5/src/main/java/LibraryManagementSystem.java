import com.mongodb.client.*;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Arrays;
import java.util.Date;

public class LibraryManagementSystem {

    private static final String DATABASE_NAME = "library";
    private static final String AUTHORS_COLLECTION = "authors";
    private static final String BOOKS_COLLECTION = "books";

    public static void main(String[] args) {
        // Connect to MongoDB
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017/");
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);

        // Insert authors
        insertAuthors(database);

        // Insert books with references to authors
        insertBooks(database);

        // Retrieve and display book information along with author details
        retrieveBooksWithAuthors(database);

        // Close the connection
        mongoClient.close();
    }

    private static void insertAuthors(MongoDatabase database) {
        MongoCollection<Document> authorsCollection = database.getCollection(AUTHORS_COLLECTION);

        Document author1 = new Document("name", "J.K. Rowling")
                .append("biography", "British author, best known for the Harry Potter series.");
        Document author2 = new Document("name", "George R.R. Martin")
                .append("biography", "American novelist and short story writer, known for A Song of Ice and Fire.");

        authorsCollection.insertMany(Arrays.asList(author1, author2));
    }

    private static void insertBooks(MongoDatabase database) {
        MongoCollection<Document> authorsCollection = database.getCollection(AUTHORS_COLLECTION);
        MongoCollection<Document> booksCollection = database.getCollection(BOOKS_COLLECTION);

        Document author1 = authorsCollection.find(Filters.eq("name", "J.K. Rowling")).first();
        Document author2 = authorsCollection.find(Filters.eq("name", "George R.R. Martin")).first();

        Document book1 = new Document("title", "Harry Potter and the Philosopher's Stone")
                .append("publication_date", new Date(97, 5, 26))
                .append("genre", "Fantasy")
                .append("author_id", author1.getObjectId("_id"));

        Document book2 = new Document("title", "A Game of Thrones")
                .append("publication_date", new Date(96, 7, 6))
                .append("genre", "Fantasy")
                .append("author_id", author2.getObjectId("_id"));

        booksCollection.insertMany(Arrays.asList(book1, book2));
    }

    private static void retrieveBooksWithAuthors(MongoDatabase database) {
        MongoCollection<Document> booksCollection = database.getCollection(BOOKS_COLLECTION);

        MongoCursor<Document> cursor = booksCollection.aggregate(Arrays.asList(
                Aggregates.lookup(AUTHORS_COLLECTION, "author_id", "_id", "author_details"),
                Aggregates.unwind("$author_details"),
                Aggregates.project(Projections.fields(
                        Projections.include("title", "publication_date", "genre"),
                        Projections.computed("author_name", "$author_details.name"),
                        Projections.computed("author_biography", "$author_details.biography")
                ))
        )).iterator();

        try {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                System.out.println(doc.toJson());
            }
        } finally {
            cursor.close();
        }
    }
}