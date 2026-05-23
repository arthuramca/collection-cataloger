package com.arthas.cataloger.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public class BookLookupService {

    private static final String OPEN_LIBRARY_URL = "https://openlibrary.org/api/books";

    private final HttpClient httpClient;

    public BookLookupService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public BookInfo lookup(String isbn) throws Exception {
        String cleanIsbn = isbn.replaceAll("[^0-9X]", "");
        if (cleanIsbn.length() != 10 && cleanIsbn.length() != 13) {
            throw new Exception("ISBN inválido. Use 10 ou 13 dígitos.");
        }

        String url = OPEN_LIBRARY_URL + "?bibkeys=ISBN:" + cleanIsbn + "&format=json&jscmd=data";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Erro HTTP " + response.statusCode() + " ao consultar Open Library.");
        }

        return parseResponse(response.body(), cleanIsbn);
    }

    private BookInfo parseResponse(String json, String isbn) throws Exception {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        String key = "ISBN:" + isbn;
        if (!root.has(key)) {
            throw new Exception("ISBN não encontrado na Open Library.");
        }

        JsonObject book = root.getAsJsonObject(key);
        BookInfo info = new BookInfo();
        info.title = getString(book, "title");

        if (book.has("authors")) {
            JsonArray authors = book.getAsJsonArray("authors");
            if (authors.size() > 0) {
                info.author = getString(authors.get(0).getAsJsonObject(), "name");
            }
        }

        if (book.has("publishers")) {
            JsonArray publishers = book.getAsJsonArray("publishers");
            if (publishers.size() > 0) {
                info.publisher = getString(publishers.get(0).getAsJsonObject(), "name");
            }
        }

        info.publishYear = getString(book, "publish_date");
        return info;
    }

    public String downloadCover(String isbn) throws Exception {
        String cleanIsbn = isbn.replaceAll("[^0-9X]", "");
        if (cleanIsbn.length() != 10 && cleanIsbn.length() != 13) {
            throw new Exception("ISBN inválido para baixar capa.");
        }

        String url = "https://covers.openlibrary.org/b/isbn/" + cleanIsbn + "-L.jpg";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new Exception("Capa não encontrada (HTTP " + response.statusCode() + ").");
        }

        byte[] bytes = response.body();
        // Open Library retorna GIF de 1x1 pixel quando a capa não existe
        if (bytes.length < 2000) {
            throw new Exception("Capa não disponível para este ISBN na Open Library.");
        }

        Path coversDir = Paths.get(System.getProperty("user.home"), "collection-cataloger", "covers");
        Files.createDirectories(coversDir);
        Path coverFile = coversDir.resolve(cleanIsbn + ".jpg");
        Files.write(coverFile, bytes);

        return coverFile.toAbsolutePath().toString();
    }

    private String getString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsString() : "";
    }

    public static class BookInfo {
        public String title = "";
        public String author = "";
        public String publisher = "";
        public String publishYear = "";
    }
}
