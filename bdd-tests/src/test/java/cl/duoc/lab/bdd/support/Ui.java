package cl.duoc.lab.bdd.support;

import cl.duoc.lab.bdd.config.TestConfig;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Utilitario central de Selenium. Mantiene un solo WebDriver por escenario,
 * aplica el proxy ZAP cuando esta configurado y entrega helpers de espera
 * robustos (la app objetivo es una SPA Angular con UI que carga asincrona).
 *
 * Localizadores calibrados contra la version de Juice Shop del lab
 * (DOM real capturado con la sonda Probe.java en evidencias/dom-*.html).
 */
public final class Ui {

    private static WebDriver driver;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(20);

    private Ui() {
    }

    public static WebDriver driver() {
        if (driver == null) {
            driver = build();
        }
        return driver;
    }

    public static WebDriver current() {
        return driver;
    }

    public static void quit() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {
            }
            driver = null;
        }
    }

    /* ------------------------------------------------------------------ */
    /* Localizadores usados por los steps                                   */
    /* ------------------------------------------------------------------ */

    public static By by(String css) {
        return By.cssSelector(css);
    }

    public static By byId(String id) {
        return By.id(id);
    }

    /* Grilla de productos (home) y resultados de busqueda */
    public static final By PRODUCT_CARD  = By.cssSelector("app-product");
    public static final By PRODUCT_NAME  = By.cssSelector("app-product .name");
    public static final By PRODUCT_PRICE = By.cssSelector("app-product .price");
    public static final By RESULT_NAME   = By.cssSelector("app-product");

    /* Buscador: boton toggle + input de la barra */
    public static final By SEARCH_TOGGLE = By.cssSelector("#searchQuery .search-toggle");
    public static final By SEARCH_INPUT  = By.cssSelector("#searchQuery .search-container input");

    /* Login */
    public static final By EMAIL_FIELD    = By.id("email");
    public static final By PASSWORD_FIELD = By.id("password");
    public static final By LOGIN_BUTTON   = By.id("loginButton");
    public static final By ACCOUNT_MENU   = By.id("navbarAccount");
    public static final By LOGOUT_BUTTON  = By.id("navbarLogoutButton");

    /* Registro */
    public static final By REG_EMAIL    = By.id("emailControl");
    public static final By REG_PASSWORD = By.id("passwordControl");
    public static final By REG_REPEAT   = By.id("repeatPasswordControl");
    public static final By REG_SEC_Q    = By.cssSelector("mat-select");
    public static final By REG_SEC_OPT  = By.cssSelector("mat-option");
    public static final By REG_SEC_ANS  = By.id("securityAnswerControl");
    public static final By REG_BUTTON   = By.id("registerButton");

    /* Carrito */
    public static final By CART_BUTTON  = By.cssSelector("button[aria-label='Show the shopping cart']");
    public static final By CART_COUNTER = By.cssSelector("button[aria-label='Show the shopping cart'] .fa-layers-counter");
    public static final By ADD_BUTTON   = By.cssSelector("button[aria-label='Add to Basket']");
    public static final By CART_ROWS    = By.cssSelector("mat-row");
    public static final By CART_CELLS   = By.cssSelector("mat-cell");
    public static final By CART_TOTAL   = By.id("price");

    /* ------------------------------------------------------------------ */
    /* Navegacion                                                          */
    /* ------------------------------------------------------------------ */

    public static void open(String path) {
        driver().get(TestConfig.BASE_URL + path);
        WebDriverWait wait = new WebDriverWait(driver(), DEFAULT_TIMEOUT);
        // la SPA angular necesita que el JS termine de boot
        wait.until(d -> ((JavascriptExecutor) d)
                .executeScript("return document.readyState").equals("complete"));
    }

    /** Cierra el dialogo de bienvenida y el banner de cookies si aparecen. */
    public static void dismissBanners() {
        clickIfPresent(By.cssSelector(".close-dialog"));
        for (String selector : List.of("#cookiesButton", ".cc-btn", "button.cc-dismiss")) {
            clickIfPresent(By.cssSelector(selector));
        }
        sleep(500);
    }

    /* ------------------------------------------------------------------ */
    /* Helpers de espera                                                   */
    /* ------------------------------------------------------------------ */

    public static WebDriverWait waitDefault() {
        return new WebDriverWait(driver(), DEFAULT_TIMEOUT);
    }

    public static WebElement clickable(By by) {
        return waitDefault().until(ExpectedConditions.elementToBeClickable(by));
    }

    public static WebElement visible(By by) {
        return waitDefault().until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    public static void clickIfPresent(By by) {
        try {
            new WebDriverWait(driver(), Duration.ofSeconds(3))
                    .until(ExpectedConditions.elementToBeClickable(by))
                    .click();
        } catch (Exception ignored) {
        }
    }

    public static boolean isDisplayed(By by) {
        try {
            WebElement el = new WebDriverWait(driver(), Duration.ofSeconds(3))
                    .until(ExpectedConditions.presenceOfElementLocated(by));
            return el.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public static int count(By by) {
        return driver().findElements(by).size();
    }

    /**
     * Busca un elemento probando varios selectores en orden (resiliencia ante
     * diferencias de versiones del DOM de Juice Shop).
     */
    public static WebElement first(By... candidates) {
        Exception last = null;
        for (By by : candidates) {
            try {
                return waitDefault().until(ExpectedConditions.visibilityOfElementLocated(by));
            } catch (Exception e) {
                last = e;
            }
        }
        throw (last instanceof RuntimeException re) ? re : new RuntimeException(last);
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /* ------------------------------------------------------------------ */
    /* Busqueda (compartida por UC-02/03 y UC-10)                          */
    /* ------------------------------------------------------------------ */

    /** Abre la barra de busqueda, escribe el termino letra a letra y confirma. */
    public static void buscar(String termino) {
        clickable(SEARCH_TOGGLE);
        WebElement input = visible(SEARCH_INPUT);
        input.click();
        input.clear();
        for (char c : termino.toCharArray()) {
            input.sendKeys(String.valueOf(c));
            sleep(40);
        }
        sleep(300);
        input.sendKeys(Keys.ENTER);
        sleep(2500);
    }

    /* ------------------------------------------------------------------ */
    /* Verificacion de texto en pagina (mensajes inline y snack bars)      */
    /* ------------------------------------------------------------------ */

    /** true si cualquier elemento visible contiene el texto indicado. */
    public static boolean textoVisible(String texto) {
        try {
            return !driver().findElements(By.xpath(
                    "//*[contains(text(),'" + texto.replace("'", "&apos;") + "')]")).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /** true si el texto aparece en un snack bar (notificacion emergente). */
    public static boolean snackMuestra(String texto) {
        try {
            return driver().findElements(By.xpath(
                            "//*[contains(@class,'snack-bar')]"))
                    .stream()
                    .flatMap(el -> el.findElements(By.xpath(".//*")).stream())
                    .anyMatch(el -> el.getText().toLowerCase().contains(texto.toLowerCase()));
        } catch (Exception e) {
            return false;
        }
    }

    /* ------------------------------------------------------------------ */
    /* Alertas JS (necesario para el caso XSS)                             */
    /* ------------------------------------------------------------------ */

    public static Optional<Alert> waitForAlert(Duration timeout) {
        try {
            Alert alert = new WebDriverWait(driver(), timeout).until(d -> {
                try {
                    return d.switchTo().alert();
                } catch (NoAlertPresentException e) {
                    return null;
                }
            });
            return Optional.ofNullable(alert);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /* ------------------------------------------------------------------ */
    /* Construcciones del WebDriver                                        */
    /* ------------------------------------------------------------------ */

    private static WebDriver build() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--window-size=1400,900",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu");

        if (!TestConfig.ZAP_PROXY.isBlank()) {
            String proxyHost = TestConfig.ZAP_PROXY.startsWith("http")
                    ? TestConfig.ZAP_PROXY
                    : "http://" + TestConfig.ZAP_PROXY;
            Proxy proxy = new Proxy();
            proxy.setHttpProxy(proxyHost).setSslProxy(proxyHost);
            options.setProxy(proxy);
            // ZAP intercepta TLS y presenta su propio certificado
            options.addArguments("--ignore-certificate-errors");
        }

        try {
            if (!TestConfig.SELENIUM_URL.isBlank()) {
                return new RemoteWebDriver(new URL(TestConfig.SELENIUM_URL), options);
            }
        } catch (Exception e) {
            throw new RuntimeException("No se pudo conectar a Selenium Grid: "
                    + TestConfig.SELENIUM_URL, e);
        }
        return new ChromeDriver(options); // fallback para ejecucion sin docker
    }
}
