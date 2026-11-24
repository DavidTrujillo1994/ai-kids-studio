package com.aikids.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.aikids.application.port.out.VideoProducerPort;
import com.aikids.domain.episode.Episode;
import com.aikids.domain.production.JobId;
import com.aikids.domain.production.Stage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Recorre la aplicación real (HTTP, SQLite, migraciones); solo el motor de video es simulado. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class ApiFlowTest {

    @TempDir
    static Path root;

    @Value("${local.server.port}")
    int port;
    @Autowired
    FakeProducer producer;

    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeAll
    static void assets() throws IOException {
        write("media/characters/bruno/ficha.json", "{\"name\":\"Bruno\",\"personality\":\"Bromista\",\"designVersion\":2}");
        write("media/characters/bruno/rig.json", "{}");
        write("media/characters/bruno/ojos/happy.svg", "");
        write("media/characters/bruno/bocas/happy.svg", "");
        write("media/characters/animaciones.json", "{\"idle\":{},\"walk\":{\"gait\":1},\"run\":{\"gait\":2},\"wave\":{},\"talk\":{},\"jump\":{}}");
        write("media/environments/ambientes.json", "{\"dia\":{},\"noche\":{}}");
        write("media/environments/bosque-central/miniatura.png", "png");
        write("media/direccion.json", "{\"rules\":[{\"words\":[\"corr\"],\"animations\":[\"jump\"],\"expression\":\"happy\",\"gait\":\"run\"}],\"neutral\":[\"talk\"]}");
        write("media/plantillas.json", "[{\"id\":\"saludo\",\"name\":\"Saludo\",\"roles\":1,\"script\":{\"environment\":\"bosque-central\",\"ambience\":\"noche\",\"characters\":[{\"id\":\"rol-1\",\"x\":0}],"
                + "\"steps\":[{\"do\":\"say\",\"character\":\"rol-1\",\"text\":\"¡Hola, amigos! Soy {1}.\",\"animation\":\"wave\"},{\"do\":\"scene\",\"environment\":\"bosque-central\",\"ambience\":\"dia\",\"positions\":{\"rol-1\":400}},"
                + "{\"do\":\"walk\",\"character\":\"rol-1\",\"to\":900,\"gait\":\"run\"}]}}]");
        write("media/environments/bosque-central/escenario.json", "{}");
        write("media/episodes/piloto-01/script/guion.json", "{\"id\":\"piloto-01\",\"title\":\"Piloto\",\"environment\":\"bosque-central\","
                + "\"characters\":[{\"id\":\"bruno\",\"x\":0}],\"steps\":[{\"do\":\"say\",\"character\":\"bruno\",\"text\":\"¡Hola!\"}]}");
        write("media/episodes/piloto-01/render/final.mp4", "0123456789");
        Files.createDirectories(root.resolve("data"));
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("aikids.root", () -> root.toString());
    }

    private static void write(String file, String content) throws IOException {
        Path p = root.resolve(file);
        Files.createDirectories(p.getParent());
        Files.writeString(p, content);
    }

    private HttpResponse<String> call(String method, String path, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (headers.length > 0) {
            request.headers(headers);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String seriesId() throws Exception {
        return field(call("GET", "/api/v1/series", null).body(), "id");
    }

    private static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\":\"([^\"]*)\"").matcher(json);
        assertThat(m.find()).as("%s en %s", name, json).isTrue();
        return m.group(1);
    }

    private String waitFor(String episode, String... statuses) throws Exception {
        String body = "";
        for (int i = 0; i < 100; i++) {
            body = call("GET", "/api/v1/episodes/" + episode, null).body();
            for (String s : statuses) {
                if (body.contains("\"status\":\"" + s + "\",\"editable\"")) {
                    return body;
                }
            }
            Thread.sleep(50);
        }
        throw new AssertionError("No llegó a " + String.join("/", statuses) + ": " + body);
    }

    private String newEpisode(String title) throws Exception {
        HttpResponse<String> r = call("POST", "/api/v1/series/" + seriesId() + "/episodes",
                "{\"title\":\"" + title + "\",\"idea\":\"x\",\"template\":\"saludo\",\"cast\":[\"bruno\"]}");
        assertThat(r.statusCode()).isEqualTo(201);
        return field(r.body(), "id");
    }

    @Test
    void theFirstStartBringsTheSeriesTheCharacterAndTheExistingEpisode() throws Exception {
        String series = seriesId();
        assertThat(call("GET", "/api/v1/series/" + series + "/characters", null).body()).contains("\"name\":\"Bruno\"", "\"designVersion\":2");
        assertThat(call("GET", "/api/v1/series/" + series + "/episodes", null).body()).contains("\"title\":\"Piloto\"", "PENDING_APPROVAL");
    }

    @Test
    void fullPathFromIdeaToExport() throws Exception {
        String id = newEpisode("La semilla");
        String created = call("GET", "/api/v1/episodes/" + id, null).body();
        assertThat(created).contains("¡Hola, amigos! Soy Bruno.", "\"problems\":[]", "SCRIPT_READY");
        assertThat(call("POST", "/api/v1/episodes/" + id + "/approval", "{\"decision\":\"APPROVE\"}").statusCode()).isEqualTo(409);

        assertThat(call("POST", "/api/v1/episodes/" + id + "/production", null).statusCode()).isEqualTo(202);
        assertThat(waitFor(id, "PENDING_APPROVAL")).contains("\"progress\":100", "SUCCEEDED");
        assertThat(call("POST", "/api/v1/episodes/" + id + "/export", null).statusCode()).isEqualTo(409);

        assertThat(call("POST", "/api/v1/episodes/" + id + "/approval", "{\"decision\":\"REJECT\",\"comments\":\"\"}").statusCode()).isEqualTo(400);
        assertThat(call("POST", "/api/v1/episodes/" + id + "/approval", "{\"decision\":\"REJECT\",\"comments\":\"Muy corto\"}").statusCode()).isEqualTo(204);
        call("POST", "/api/v1/episodes/" + id + "/production", null);
        waitFor(id, "PENDING_APPROVAL");
        assertThat(call("POST", "/api/v1/episodes/" + id + "/approval", "{\"decision\":\"APPROVE\"}").statusCode()).isEqualTo(204);
        assertThat(call("GET", "/api/v1/episodes/" + id, null).body()).contains("Muy corto", "APPROVED", "\"editable\":false");

        String folder = field(call("POST", "/api/v1/episodes/" + id + "/export", null).body(), "folder");
        assertThat(root.resolve(folder).resolve("video.mp4")).exists();
    }

    @Test
    void theVideoSupportsSeeking() throws Exception {
        String series = call("GET", "/api/v1/series/" + seriesId() + "/episodes", null).body();
        Matcher m = Pattern.compile("\"id\":\"([^\"]*)\",\"title\":\"Piloto\"").matcher(series);
        assertThat(m.find()).isTrue();
        HttpResponse<String> r = call("GET", "/api/v1/episodes/" + m.group(1) + "/preview", null, "Range", "bytes=2-4");
        assertThat(r.statusCode()).isEqualTo(206);
        assertThat(r.body()).isEqualTo("234");
    }

    @Test
    void aBadScriptIsSavedWithItsProblemsButNotProduced() throws Exception {
        String id = newEpisode("Con errores");
        String body = "{\"title\":\"Con errores\",\"idea\":\"\",\"environment\":\"bosque-central\",\"script\":{\"cast\":[{\"character\":\"bruno\",\"startX\":0}],"
                + "\"steps\":[{\"type\":\"PLAY\",\"character\":\"bruno\",\"animation\":\"volar\",\"expression\":\"happy\"}]}}";
        assertThat(call("PUT", "/api/v1/episodes/" + id, body).body()).contains("\"step\":0", "volar");
        HttpResponse<String> r = call("POST", "/api/v1/episodes/" + id + "/production", null);
        assertThat(r.statusCode()).isEqualTo(422);
        assertThat(r.body()).contains("GUION_INVALIDO", "\"problems\"");
    }

    @Test
    void anEngineFailureIsExplainedAndCanBeRetried() throws Exception {
        String id = newEpisode("Falla");
        producer.fail = true;
        call("POST", "/api/v1/episodes/" + id + "/production", null);
        assertThat(waitFor(id, "FAILED")).contains("Falta Godot");
        producer.fail = false;
        call("POST", "/api/v1/episodes/" + id + "/production", null);
        waitFor(id, "PENDING_APPROVAL");
        assertThat(call("DELETE", "/api/v1/episodes/" + id, null).statusCode()).isEqualTo(204);
        assertThat(call("GET", "/api/v1/episodes/" + id, null).statusCode()).isEqualTo(404);
    }

    @Test
    void aTemplateBringsItsPlacesAndMovements() throws Exception {
        String body = call("GET", "/api/v1/episodes/" + newEpisode("Con plantilla"), null).body();
        assertThat(body).contains("Soy Bruno.", "\"ambience\":\"noche\"", "\"type\":\"SCENE\"", "\"gait\":\"run\"", "\"positions\":[{\"character\":\"bruno\",\"startX\":400}]");
        assertThat(call("GET", "/api/v1/story-templates", null).body()).contains("\"id\":\"saludo\"");
        assertThat(call("POST", "/api/v1/series/" + seriesId() + "/episodes", "{\"title\":\"x\",\"template\":\"saludo\",\"cast\":[]}").statusCode()).isEqualTo(400);
        assertThat(call("GET", "/api/v1/catalog/environments/bosque-central/view", null).statusCode()).isEqualTo(200);
        assertThat(call("GET", "/api/v1/catalog/environments/..%2F..%2Fdata/view", null).statusCode()).isGreaterThanOrEqualTo(400);
    }

    @Test
    void suggestsMovementsThatFitTheWords() throws Exception {
        String script = "{\"environment\":\"bosque-central\",\"script\":{\"cast\":[{\"character\":\"bruno\",\"startX\":0}],\"steps\":["
                + "{\"type\":\"SAY\",\"character\":\"bruno\",\"text\":\"¡A correr!\",\"animation\":\"talk\",\"expression\":\"normal\"},"
                + "{\"type\":\"WALK\",\"character\":\"bruno\",\"to\":500}]}}";
        assertThat(call("POST", "/api/v1/movement-suggestions", script).body()).contains("\"animation\":\"jump\"", "\"gait\":\"run\"");
    }

    @Test
    void rejectsPathsMalformedDataAndUnknownIds() throws Exception {
        assertThat(call("POST", "/api/v1/series/" + seriesId() + "/episodes", "{\"title\":\"x\",\"environment\":\"../../x\"}").statusCode()).isEqualTo(400);
        assertThat(call("POST", "/api/v1/series/" + seriesId() + "/episodes", "{no json").statusCode()).isEqualTo(400);
        assertThat(call("GET", "/api/v1/episodes/no-es-un-id", null).statusCode()).isEqualTo(400);
        assertThat(call("GET", "/api/v1/episodes/00000000-0000-0000-0000-000000000000", null).statusCode()).isEqualTo(404);
        assertThat(call("GET", "/api/v1/nada", null).statusCode()).isEqualTo(404);
    }

    @Test
    void approvedDesignIsLocked() throws Exception {
        String series = field(call("POST", "/api/v1/series", "{\"name\":\"Otra\"}").body(), "id");
        String id = field(call("POST", "/api/v1/series/" + series + "/characters", "{\"assetKey\":\"bruno\",\"role\":\"PROTAGONISTA\"}").body(), "id");
        assertThat(call("POST", "/api/v1/series/" + series + "/characters", "{\"assetKey\":\"bruno\",\"role\":\"PROTAGONISTA\"}").statusCode()).isEqualTo(400);
        assertThat(call("POST", "/api/v1/characters/" + id + "/design-approval", null).body()).contains("APROBADO");
        assertThat(call("PUT", "/api/v1/characters/" + id, "{\"name\":\"Pepe\"}").statusCode()).isEqualTo(409);
    }

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        FakeProducer fakeProducer() {
            return new FakeProducer();
        }
    }

    static class FakeProducer implements VideoProducerPort {
        volatile boolean fail;

        @Override
        public void produce(JobId jobId, Episode episode, ProgressListener listener) throws ProductionFailedException {
            listener.onProgress(Stage.VOICES, 20, "Voz 1 de 1");
            if (fail) {
                throw new ProductionFailedException("Falta Godot", "detalle");
            }
            try {
                write("media/episodes/" + episode.folder() + "/render/final.mp4", "video");
            } catch (IOException e) {
                throw new ProductionFailedException("disco", e.toString());
            }
            listener.onProgress(Stage.VALIDATION, 97, "Validando");
        }

        @Override
        public void cancel(JobId jobId) {
        }
    }
}
