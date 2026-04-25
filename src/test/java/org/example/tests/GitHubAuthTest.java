package org.example.tests;

import org.example.methods.GitHubRestMethods;
import org.example.dto.Repository;
import org.example.dto.User;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class GitHubAuthTest extends GitHubRestMethods {

    private String repoOwner;
    private String repoName;

    @Test
    public void testAuthentication() throws Exception {
        var user = executeGet("/user", User.class);
        
        // проверка успешного входа и наличие логина в ответе
        Assert.assertNotNull(user.login, "нет логина");
        
        // проверка хедеров
        Assert.assertNotNull(lastRateLimitHeader, "нет лимитов");
        Assert.assertTrue(getResponseHeader("content-type").contains("application/json"), "ошибка content-type");
        Assert.assertTrue(getRequestHeader("authorization").contains("Bearer"), "ошибка authorization");
        
        repoOwner = user.login;
    }

    @Test(dependsOnMethods = "testAuthentication")
    public void testCreateRepository() throws Exception {
        repoName = "test-repo-" + System.currentTimeMillis();
        // создание нового репозитория
        var repo = executePost("/user/repos", new Repository(repoName, "desc", false), Repository.class);

        Assert.assertEquals(lastStatusCode, 201, "статус не 201");
        Assert.assertEquals(repo.name, repoName, "имя не то");
        Assert.assertTrue(repo.id > 0, "нет id");
    }

    // проверка получения репозитория
    @Test(dependsOnMethods = "testCreateRepository")
    public void testGetRepository() throws Exception {
        var repo = executeGet("/repos/" + repoOwner + "/" + repoName, Repository.class);

        Assert.assertEquals(lastStatusCode, 200, "статус не 200");
        Assert.assertEquals(repo.name, repoName, "имя кривое");
    }

    @DataProvider(name = "descProvider")
    public Object[][] descriptions() {
        return new Object[][] {
            {"новое описание 1"},
            {"новое описание 2"}
        };
    }

    // проверка обновления репозитория
    @Test(dependsOnMethods = "testCreateRepository", dataProvider = "descProvider")
    public void testUpdateRepository(String newDesc) throws Exception {
        var repo = executePatch("/repos/" + repoOwner + "/" + repoName, new Repository(repoName, newDesc, false), Repository.class);

        Assert.assertEquals(lastStatusCode, 200, "ошибка при patch");
        Assert.assertEquals(repo.description, newDesc, "описание старое");
    }

    // проверка невозможности создать репозиторий с существующим именем
    @Test(dependsOnMethods = "testCreateRepository")
    public void testNegativeCreateRepository() {
        try {
            executePost("/user/repos", new Repository(repoName, "дубль", false), Repository.class);
            Assert.fail("ожидали ошибку, а репозиторий создался");
        } catch (Exception e) {
            Assert.assertEquals(lastStatusCode, 422, "ждали 422");
        }
    }

    // проверка с неверным токеном
    @Test
    public void testNegativeUnauthorized() {
        var good = token;
        token = "invalid_token";
        try {
            executeGet("/user", User.class);
            Assert.fail("ожидали ошибку авторизации");
        } catch (Exception e) {
            Assert.assertEquals(lastStatusCode, 401, "ждали 401");
        } finally {
            token = good;
        }
    }

    // проверка удаления репозитория
    @Test(dependsOnMethods = {"testUpdateRepository", "testGetRepository", "testNegativeCreateRepository"})
    public void testDeleteRepository() throws Exception {
        executeDelete("/repos/" + repoOwner + "/" + repoName);
        Assert.assertEquals(lastStatusCode, 204, "не 204");
    }

    // проверка 404 удаленного ресурса
    @Test(dependsOnMethods = "testDeleteRepository")
    public void testGetDeletedRepository() throws InterruptedException {
        Thread.sleep(1500);
        try {
            executeGet("/repos/" + repoOwner + "/" + repoName, Repository.class);
            Assert.fail("ожидали 404, но репо все еще существует");
        } catch (Exception e) {
            Assert.assertEquals(lastStatusCode, 404, "ждали 404");
        }
    }
}