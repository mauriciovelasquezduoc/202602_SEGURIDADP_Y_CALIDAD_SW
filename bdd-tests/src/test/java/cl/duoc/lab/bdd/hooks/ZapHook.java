package cl.duoc.lab.bdd.hooks;

import cl.duoc.lab.bdd.config.TestConfig;
import io.cucumber.java.AfterAll;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hook que corre UNA VEZ al final de la suite.
 *
 * Como el navegador de Selenium navega TRAVES del proxy de ZAP, todo el
 * trafico generado por los tests ya fue escaneado pasivamente. Aqui se
 * completa el DAST:
 *   1. espera que el escaneo pasivo drene su cola
 *   2. corre el spider de ZAP contra Juice Shop
 *   3. lanza el Active Scan (si ZAP_ACTIVE_SCAN=true)
 *   4. genera el reporte HTML + un JSON crudo de alertas en /evidencias
 */
public class ZapHook {

    private static final long MAX_PSCAN_MS  = 2 * 60 * 1000;   // drenar cola pasiva: 2 min
    private static final long MAX_SPIDER_MS = 10 * 60 * 1000;  // spider: 10 min
    private static final long MAX_ASCAN_MS  = 20 * 60 * 1000;  // active scan: 20 min

    private static boolean ejecutado = false;

    @AfterAll
    public static void generarReporteZap() {
        if (ejecutado || TestConfig.ZAP_API_URL.isBlank()) {
            return;
        }
        ejecutado = true;
        try {
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();

            String target = enc(TestConfig.BASE_URL);

            System.out.println("[ZAP] esperando que drene el escaneo pasivo (max 2 min)...");
            esperarValor(http, "/JSON/pscan/view/recordsToScan/", "recordsToScan",
                    valor -> valor <= 0, MAX_PSCAN_MS);

            System.out.println("[ZAP] lanzando spider...");
            get(http, "/JSON/spider/action/scan/?url=" + target);
            esperarValor(http, "/JSON/spider/view/status/", "status",
                    valor -> valor >= 100, MAX_SPIDER_MS);
            System.out.println("[ZAP] spider terminado.");

            if (TestConfig.ZAP_ACTIVE_SCAN) {
                System.out.println("[ZAP] lanzando Active Scan (puede tardar varios minutos)...");
                get(http, "/JSON/ascan/action/scan/?url=" + target);
                esperarValor(http, "/JSON/ascan/view/status/", "status",
                        valor -> valor >= 100, MAX_ASCAN_MS);
                System.out.println("[ZAP] Active Scan terminado.");
            } else {
                System.out.println("[ZAP] Active Scan deshabilitado (ZAP_ACTIVE_SCAN != true).");
            }

            guardar(http, "/OTHER/core/other/htmlreport/",
                    TestConfig.EVIDENCIAS_DIR + "/zap-report.html");
            guardar(http, "/JSON/core/view/alerts/",
                    TestConfig.EVIDENCIAS_DIR + "/zap-alerts.json");

            System.out.println("[ZAP] reportes generados en " + TestConfig.EVIDENCIAS_DIR
                    + " (zap-report.html, zap-alerts.json)");
        } catch (Exception e) {
            System.err.println("[ZAP] aviso: no se pudo completar el reporte: " + e.getMessage());
        }
    }

    /* ---------------------------- helpers ------------------------------ */

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /**
     * GET a la API de ZAP y extrae el entero asociado a la clave pedida:
     *   {"status":"85"} -> 85 ; {"recordsToScan":"12"} -> 12
     */
    private static int valorDe(HttpClient http, String path, String key) {
        try {
            String body = get(http, path);
            Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?(\\d+)").matcher(body);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {
        }
        return -1;
    }

    /** Polling del endpoint hasta que la condicion se cumpla (o expire el tiempo). */
    private static void esperarValor(HttpClient http, String path, String key,
                                     java.util.function.IntPredicate condicion,
                                     long maxMs)
            throws InterruptedException {
        long limite = System.currentTimeMillis() + maxMs;
        while (System.currentTimeMillis() < limite) {
            int valor = valorDe(http, path, key);
            if (valor >= 0 && condicion.test(valor)) {
                return;
            }
            Thread.sleep(5000);
        }
        System.err.println("[ZAP] se alcanzo el tiempo limite; se continua con el reporte.");
    }

    private static String get(HttpClient http, String path) throws Exception {
        String url = TestConfig.ZAP_API_URL + path
                + (path.contains("?") ? "&" : "?") + "apikey=" + enc(TestConfig.ZAP_API_KEY);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(120))
                .GET()
                .build();
        // 3 reintentos ante cortes de la API de ZAP
        Exception ultimo = null;
        for (int i = 0; i < 3; i++) {
            try {
                return http.send(req, HttpResponse.BodyHandlers.ofString()).body();
            } catch (Exception e) {
                ultimo = e;
                Thread.sleep(2000);
            }
        }
        throw ultimo;
    }

    private static void guardar(HttpClient http, String path, String destino) {
        try {
            String url = TestConfig.ZAP_API_URL + path
                    + (path.contains("?") ? "&" : "?") + "apikey=" + enc(TestConfig.ZAP_API_KEY);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(180))
                    .GET()
                    .build();
            HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            byte[] data = res.body() == null ? new byte[0] : res.body();
            Path salida = Path.of(destino);
            Files.createDirectories(salida.getParent());
            Files.write(salida, data);
            System.out.println("[ZAP] escrito: " + destino + " (" + data.length + " bytes)");
        } catch (Exception e) {
            System.err.println("[ZAP] no se pudo guardar " + destino + ": " + e.getMessage());
        }
    }
}
