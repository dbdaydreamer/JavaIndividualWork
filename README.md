Запуск тестов   
Инструкция:

1. Токен
   Personal Access Token от GitHub с правом `delete_repo`.
   GitHub -> Settings -> Developer settings -> Personal access tokens (classic).
   Create token с галочкой delete_repo и repo для общих прав.
   Вставить в файл `src/main/resources/config.properties` в переменную `github.token`.

2. Тест
   mvn clean test или запустить `GitHubAuthTest.java`


testAuthentication - вход и получение логина
testNegativeUnauthorized - ошибка 401 при кривом токене
testCreateRepository - создание репо (201)
testGetRepository - проверка существования (200)
testNegativeCreateRepository - ошибка 422 на дубликат имени
testUpdateRepository - обновление описания (200)
testDeleteRepository - удаление репо (204)