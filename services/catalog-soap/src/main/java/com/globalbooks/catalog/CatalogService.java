package com.globalbooks.catalog;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

// Task 5: CatalogService SOAP Implementation
// This Java class implements the SOAP endpoint for catalog operations.
// Uses JAX-WS annotations for contract-first design.
// Viva Explanation: Demonstrates WS-* services (ILO2); @WebService enables WSDL generation.
// In-memory data store for demo; in production, use database.

@WebService(serviceName = "CatalogService", portName = "CatalogServicePort",
            targetNamespace = "http://globalbooks.com/catalog",
            wsdlLocation = "WEB-INF/wsdl/CatalogService.wsdl")
public class CatalogService {

    // Demo data store
    private static final Map<String, Book> books = new HashMap<>();

    static {
        books.put("1", new Book("1", "Java SOA", "Author A", 29.99, "Technology"));
        books.put("2", new Book("2", "Microservices", "Author B", 39.99, "Technology"));
        books.put("3", new Book("3", "E-commerce", "Author C", 19.99, "Business"));
    }

    @WebMethod(operationName = "getBookById")
    public Book getBookById(@WebParam(name = "bookId") String bookId) {
        return books.get(bookId);
    }

    @WebMethod(operationName = "getBooksByCategory")
    public List<Book> getBooksByCategory(@WebParam(name = "category") String category) {
        List<Book> result = new ArrayList<>();
        for (Book book : books.values()) {
            if (book.getCategory().equalsIgnoreCase(category)) {
                result.add(book);
            }
        }
        return result;
    }

    // Inner class for Book
    public static class Book {
        private String id;
        private String title;
        private String author;
        private double price;
        private String category;

        public Book() {}

        public Book(String id, String title, String author, double price, String category) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.price = price;
            this.category = category;
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
}