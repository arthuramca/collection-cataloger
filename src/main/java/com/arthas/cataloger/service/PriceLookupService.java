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

    private static final String ML_API_URL    = "https://api.mercadolibre.com/sites/MLB/search";
    private static final String ML_SEARCH_URL = "https://lista.mercadolivre.com.br/";

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

        String trimmed = query.trim();
        String encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8);
        String url = ML_API_URL + "?q=" + encoded + "&limit=20";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            // API exige autenticação — lança exceção especial com a URL de busca
            String browserUrl = ML_SEARCH_URL + encoded.replace("+", "-");
            throw new PriceApiAuthException("API requer autenticação.", browserUrl, trimmed);
        }

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

    /** Lançada quando a API retorna 401/403 — carrega a URL para abrir no navegador. */
    public static class PriceApiAuthException extends Exception {
        public final String browserUrl;
        public final String query;

        public PriceApiAuthException(String message, String browserUrl, String query) {
            super(message);
            this.browserUrl = browserUrl;
            this.query = query;
        }
    }
}
