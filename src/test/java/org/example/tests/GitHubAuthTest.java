package org.example.tests;

import org.example.base.GitHubRestMethods;
import org.example.dto.Repository;
import org.example.dto.User;
import org.testng.Assert;
import org.testng.annotations.Test;

public class GitHubAuthTest extends GitHubRestMethods {

    private String repoOwner;
    private String repoName;

    @Test
    public void testAuthentication() throws Exception {
        var user = executeGet("/user", User.class);
        
        // проверка успешного входа и наличие логина в ответе
        Assert.assertNotNull(user.login, "нет логина");
        // проверка хедера
        Assert.assertNotNull(lastRateLimitHeader, "нет лимитов github");
        
        repoOwner = user.login;
    }

    @Test(dependsOnMethods = "testAuthentication")
    public void testCreateRepository() throws Exception {
        repoName = "test-repo-" + System.currentTimeMillis();
        // создание нового публичного репозитория
        var repo = executePost("/user/repos", new Repository(repoName, "desc", false), Repository.class);

        Assert.assertEquals(lastStatusCode, 201, "статус не 201");
        Assert.assertEquals(repo.name, repoName, "имя не то");
        Assert.assertTrue(repo.id > 0, "нет id");
    }

    // проверка получения созданного репозитория
    @Test(dependsOnMethods = "testCreateRepository")
    public void testGetRepository() throws Exception {
        var repo = executeGet("/repos/" + repoOwner + "/" + repoName, Repository.class);

        Assert.assertEquals(lastStatusCode, 200, "статус не 200");
        Assert.assertEquals(repo.name, repoName, "имя кривое");
    }

    // проверка обновления созданного репозитория
    @Test(dependsOnMethods = "testCreateRepository")
    public void testUpdateRepository() throws Exception {
        var repo = executePatch("/repos/" + repoOwner + "/" + repoName, new Repository(repoName, "new desc", false), Repository.class);

        Assert.assertEquals(lastStatusCode, 200, "ошибка при патче");
        Assert.assertEquals(repo.description, "new desc", "описание старое");
    }

    // проверка невозможности создать репозиторий с существующим именем
    @Test(dependsOnMethods = "testCreateRepository")
    public void testNegativeCreateRepository() {
        try {
            executePost("/user/repos", new Repository(repoName, "дубль", false), Repository.class);
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
        } catch (Exception e) {
            Assert.assertEquals(lastStatusCode, 401, "ждали 401");
        } finally {
            token = good; // возвращаем токен
        }
    }

    // проверка удаления созданного репозитория
    @Test(dependsOnMethods = {"testUpdateRepository", "testGetRepository", "testNegativeCreateRepository"})
    public void testDeleteRepository() throws Exception {
        executeDelete("/repos/" + repoOwner + "/" + repoName);
        Assert.assertEquals(lastStatusCode, 204, "не 204");
    }
}