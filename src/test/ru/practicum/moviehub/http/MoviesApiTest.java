package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE_URL = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;
    private static Gson gson;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        gson = new GsonBuilder().create();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        String body = response.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"));

        Movie[] movies = gson.fromJson(body, Movie[].class);
        assertEquals(0, movies.length);
    }

    @Test
    void postMovie_whenValid_returnsCreated() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("Звёздные войны: Эпизод 2 - Атака клонов");
        movie.setYear(2002);

        String json = gson.toJson(movie);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, response.statusCode());

        Movie created = gson.fromJson(response.body(), Movie.class);
        assertEquals("Звёздные войны: Эпизод 2 - Атака клонов", created.getTitle());
        assertEquals(2002, created.getYear());
    }

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {

        Movie movie = new Movie();
        movie.setTitle("Звёздные войны: Эпизод 2 - Атака клонов");
        movie.setYear(2002);

        String json = gson.toJson(movie);

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> postResponse = client.send(postRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Movie created = gson.fromJson(postResponse.body(), Movie.class);
        int id = created.getId();


        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + id))
                .GET()
                .build();

        HttpResponse<String> getResponse = client.send(getRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, getResponse.statusCode());

        Movie found = gson.fromJson(getResponse.body(), Movie.class);
        assertEquals(id, found.getId());
        assertEquals("Звёздные войны: Эпизод 2 - Атака клонов", found.getTitle());
    }

    @Test
    void deleteMovie_whenExists_returnsNoContent() throws Exception {

        Movie movie = new Movie();
        movie.setTitle("Звёздные войны: Эпизод 2 - Атака клонов");
        movie.setYear(2002);

        String json = gson.toJson(movie);

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> postResponse = client.send(postRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Movie created = gson.fromJson(postResponse.body(), Movie.class);
        int id = created.getId();


        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + id))
                .DELETE()
                .build();

        HttpResponse<String> deleteResponse = client.send(deleteRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, deleteResponse.statusCode());


        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + id))
                .GET()
                .build();

        HttpResponse<String> getResponse = client.send(getRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, getResponse.statusCode());
    }

    @Test
    void getMoviesByYear_whenMoviesExist_returnsFilteredMovies() throws Exception {

        Movie starWarsOne = new Movie();
        starWarsOne.setTitle("Звёздные войны: Эпизод 1 - Скрытая угроза");
        starWarsOne.setYear(1999);

        Movie starWarsTwo = new Movie();
        starWarsTwo.setTitle("Звёздные войны: Эпизод 2 - Атака клонов");
        starWarsTwo.setYear(2002);

        Movie starWarsThree = new Movie();
        starWarsThree.setTitle("Звёздные войны: Эпизод 3 - Месть ситхов");
        starWarsThree.setYear(2005);

        String jsonStarWarsOne = gson.toJson(starWarsOne);
        String jsonStarWarsTwo = gson.toJson(starWarsTwo);
        String jsonStarWarsThree = gson.toJson(starWarsThree);

        HttpRequest postRequest1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonStarWarsOne, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();

        HttpRequest postRequest2 = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonStarWarsTwo, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();

        HttpRequest postRequest3 = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonStarWarsThree, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();

        client.send(postRequest1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        client.send(postRequest2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        client.send(postRequest3, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));



        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies?year=2002"))
                .GET()
                .build();

        HttpResponse<String> getResponse = client.send(getRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, getResponse.statusCode());

        Movie[] movies = gson.fromJson(getResponse.body(), Movie[].class);
        assertEquals(1, movies.length);
        assertEquals("Звёздные войны: Эпизод 2 - Атака клонов", movies[0].getTitle());
        assertEquals(2002, movies[0].getYear());
    }

    @Test
    void getMoviesByYear_whenNoMovies_returnsEmptyArray() throws Exception {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies?year=1999"))
                .GET()
                .build();

        HttpResponse<String> getResponse = client.send(getRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, getResponse.statusCode());

        Movie[] movies = gson.fromJson(getResponse.body(), Movie[].class);
        assertEquals(0, movies.length);
    }

    @Test
    void postMovie_whenEmptyTitle_returnsValidationError() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("");
        movie.setYear(2002);

        String json = gson.toJson(movie);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, response.statusCode());

        String body = response.body();
        assertTrue(body.contains("Ошибка валидации"));
        assertTrue(body.contains("название не должно быть пустым"));
    }

    @Test
    void getMovieById_whenNotFound_returns404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/123"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, response.statusCode());
    }

    @Test
    void getMovieById_whenInvalidId_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/movie1"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, response.statusCode());
    }
}