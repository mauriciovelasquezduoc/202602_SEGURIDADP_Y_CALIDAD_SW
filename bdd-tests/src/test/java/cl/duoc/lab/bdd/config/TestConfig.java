package cl.duoc.lab.bdd.config;

/**
 * Configuracion central de la suite. Todos los valores se leen de variables de
 * entorno (las define docker-compose.yml) con valores por defecto pensados
 * para ejecucion local.
 */
public final class TestConfig {

    public static final String BASE_URL      = env("BASE_URL", "http://localhost:3000");
    public static final String SELENIUM_URL  = env("SELENIUM_URL", "http://selenium:4444/wd/hub");
    public static final String ZAP_PROXY     = env("ZAP_PROXY", "");
    public static final String ZAP_API_URL   = env("ZAP_API_URL", "");
    public static final String ZAP_API_KEY   = env("ZAP_API_KEY", "");
    public static final String EVIDENCIAS_DIR = env("EVIDENCIAS_DIR", "evidencias");
    public static final boolean ZAP_ACTIVE_SCAN = Boolean.parseBoolean(env("ZAP_ACTIVE_SCAN", "false"));

    private TestConfig() {
    }

    private static String env(String name, String def) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? def : value.trim();
    }
}
