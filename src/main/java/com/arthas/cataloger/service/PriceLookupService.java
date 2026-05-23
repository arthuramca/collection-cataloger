package com.arthas.cataloger.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class PriceLookupService {

    private static final String ML_SEARCH_URL = "https://api.mercadolibre.com/sites/MLB/search";

    private final HttpClient httpClient;

    public PriceLookupService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public PriceResult fetchPrice(String query) throws Exception {
        if (query == null || query.isBlank()) {
            throw new Exception("Informe o nome do item para buscar o preço.");
        }

        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        String url = ML_SEARCH_URL + "?q=" + encoded + "&limit=20";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Erro HTTP " + response.statusCode() + " ao consultar Mercado Livre.");
        }

        return parseResponse(response.body());
    }

    private PriceResult parseResponse(String json) throws Exception {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray results = root.getAsJsonArray("results");

        if (results == null || results.size() == 0) {
            throw new Exception("Nenhum resultado encontrado no Mercado Livre.");
        }

        double total = 0;
        int count = 0;
        double lowest = Double.MAX_VALUE;

        for (var element : results) {
            JsonObject item = element.getAsJsonObject();
            if (item.has("price") && !item.get("price").isJsonNull()) {
                double price = item.get("price").getAsDouble();
                if (price > 0) {
                    total += price;
                    if (price < lowest) lowest = price;
                    count++;
                }
            }
        }

        if (count == 0) {
            throw new Exception("Nenhum preço disponível nos resultados.");
        }

        PriceResult result = new PriceResult();
        result.average = total / count;
        result.lowest = lowest;
        result.sampleSize = count;
        return result;
    }

    public static class PriceResult {
        public double average;
        public double lowest;
        public int sampleSize;
    }
}
