import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class Server {
  static void respond(HttpExchange ex, int code, String body) throws IOException {
    byte[] b = body.getBytes(StandardCharsets.UTF_8);
    ex.sendResponseHeaders(code, b.length);
    try (var os = ex.getResponseBody()) { os.write(b); }
  }

  public static void main(String[] args) throws Exception {
    String version = System.getenv().getOrDefault("APP_VERSION", "dev");
    String pod = System.getenv().getOrDefault("HOSTNAME", "local");
    boolean ready = !"false".equals(System.getenv("READY_OK"));
    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
    server.createContext("/healthz", ex -> respond(ex, 200, "ok\n"));
    server.createContext("/ready", ex -> respond(ex, ready ? 200 : 503, ready ? "ready\n" : "not ready\n"));
    server.createContext("/", ex -> respond(ex, 200, "version=" + version + " pod=" + pod + "\n"));
    server.start();
  }
}
