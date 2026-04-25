package org.example.methods;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.example.configLoader.Config;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import java.io.IOException;

public class GitHubRestMethods {
    protected CloseableHttpClient httpClient;
    protected ObjectMapper mapper = new ObjectMapper();
    protected String token = Config.getProp("github.token");
    protected String baseUrl = Config.getProp("base.url");

    protected int lastStatusCode;
    protected String lastRateLimitHeader;

    @BeforeTest
    public void setup() {
        httpClient = HttpClients.createDefault();
    }

    // единый метод выполнения запросов
    private <T> T executeRequest(ClassicHttpRequest request, Class<T> clazz) throws IOException {
        request.setHeader("Authorization", "Bearer " + token);
        request.setHeader("Accept", "application/vnd.github+json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            lastStatusCode = response.getCode();
            // проверка лимитов
            var rateLimit = response.getFirstHeader("X-RateLimit-Limit");
            lastRateLimitHeader = rateLimit != null ? rateLimit.getValue() : null;

            // логирование
            System.out.println("запрос: " + request.getMethod() + " " + request.getRequestUri());
            System.out.println("статус: " + lastStatusCode);

            // проверка статуса
            if (lastStatusCode != 200 && lastStatusCode != 201 && lastStatusCode != 204) {
                throw new RuntimeException("ошибка api: " + lastStatusCode);
            }
            // проверка ответа
            if (lastStatusCode == 204 || clazz == null) return null;
            // парсинг ответа через джексон
            var type = mapper.getTypeFactory().constructType(clazz);
            return response.getEntity() != null ? mapper.readValue(response.getEntity().getContent(), type) : null;
        }
    }

    // ставим тело запроса
    private void setBody(HttpUriRequestBase request, Object body) throws IOException {
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(mapper.writeValueAsString(body), ContentType.APPLICATION_JSON));
    }

    protected <T> T executeGet(String endpoint, Class<T> clazz) throws IOException {
        return executeRequest(new HttpGet(baseUrl + endpoint), clazz);
    }

    protected <T> T executePost(String endpoint, Object body, Class<T> clazz) throws IOException {
        var request = new HttpPost(baseUrl + endpoint);
        setBody(request, body);
        return executeRequest(request, clazz);
    }

    protected <T> T executePatch(String endpoint, Object body, Class<T> clazz) throws IOException {
        var request = new HttpPatch(baseUrl + endpoint);
        setBody(request, body);
        return executeRequest(request, clazz);
    }

    protected <T> T executePut(String endpoint, Object body, Class<T> clazz) throws IOException {
        var request = new HttpPut(baseUrl + endpoint);
        setBody(request, body);
        return executeRequest(request, clazz);
    }

    protected void executeDelete(String endpoint) throws IOException {
        executeRequest(new HttpDelete(baseUrl + endpoint), null);
    }

    @AfterTest
    public void tearDown() throws IOException {
        if (httpClient != null) httpClient.close();
    }
}