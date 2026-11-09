package cl.duoc.lab.bdd;

import cl.duoc.lab.bdd.config.TestConfig;
import cl.duoc.lab.bdd.support.Ui;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Sonda diagnostica (no es un test): abre las paginas clave, agrega un
 * producto al carrito y dejan una sesion autenticada via SQL injection,
 * y escribe el DOM en evidencias/dom-*.html para calibrar localizadores.
 */
public class Probe {

    public static void main(String[] args) throws Exception {
        WebDriver d = Ui.driver();
        dump(d, "home", "/");

        // comportamiento del search-toggle: click y luego ver que pasa
        Ui.clickable(Ui.SEARCH_TOGGLE);
        Ui.sleep(2000);
        escribir("search-despues-click", d.getPageSource());
        byte[] shot = ((org.openqa.selenium.TakesScreenshot) d).getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
        Files.write(Files.createDirectories(Path.of(TestConfig.EVIDENCIAS_DIR))
                .resolve("search-despues-click.png"), shot);
        System.out.println("container display: " + css(d, "#searchQuery .search-container", "display"));
        System.out.println("container width:   " + css(d, "#searchQuery .search-container", "width"));
        System.out.println("input display:     " + css(d, "#searchQuery .search-container input", "display"));

        // agregar el primer producto al carrito y abrir el carrito con contenido
        Ui.dismissBanners();
        d.findElements(By.cssSelector("button[aria-label='Add to Basket']"))
                .stream().findFirst()
                .ifPresent(org.openqa.selenium.WebElement::click);
        Ui.sleep(2000);
        d.get(TestConfig.BASE_URL + "/#/basket");
        Ui.sleep(2000);
        escribir("basket-con-items", d.getPageSource());

        // autenticarse via SQLi (la misma ruta que usa UC-09) y abrir el menu cuenta
        d.get(TestConfig.BASE_URL + "/#/login");
        Ui.dismissBanners();
        var email = Ui.visible(Ui.EMAIL_FIELD);
        email.clear();
        email.sendKeys("' or 1=1--");
        var pass = Ui.visible(Ui.PASSWORD_FIELD);
        pass.clear();
        pass.sendKeys("x");
        pass.sendKeys(org.openqa.selenium.Keys.TAB);
        Ui.clickable(Ui.LOGIN_BUTTON).click();
        Ui.sleep(2000);
        Ui.clickIfPresent(Ui.ACCOUNT_MENU);
        Ui.sleep(1500);
        escribir("menu-cuenta", d.getPageSource());
        Ui.quit();
    }

    private static void dump(WebDriver d, String name, String path) throws Exception {
        Ui.open(path);
        Ui.dismissBanners();
        Ui.sleep(1500);
        escribir(name, d.getPageSource());
    }

    private static void escribir(String name, String html) throws Exception {
        Path dir = Files.createDirectories(Path.of(TestConfig.EVIDENCIAS_DIR));
        Files.write(dir.resolve("dom-" + name + ".html"), html.getBytes(StandardCharsets.UTF_8));
        System.out.println("[probe] dom-" + name + ".html escrito");
    }

    private static String css(WebDriver d, String selector, String prop) {
        try {
            WebElement el = d.findElement(org.openqa.selenium.By.cssSelector(selector));
            return String.valueOf(((org.openqa.selenium.JavascriptExecutor) d)
                    .executeScript(
                            "return window.getComputedStyle(arguments[0])." + prop + ";", el));
        } catch (Exception e) {
            return "n/a (" + e.getClass().getSimpleName() + ")";
        }
    }
}
