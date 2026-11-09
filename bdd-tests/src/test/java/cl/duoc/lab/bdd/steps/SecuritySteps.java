package cl.duoc.lab.bdd.steps;

import cl.duoc.lab.bdd.support.Ui;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.openqa.selenium.Alert;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pasos que validan vulnerabilidades conocidas de OWASP Juice Shop.
 * El objetivo del laboratorio es que el test FALLA solo si la app esta
 * protegida: un test "videoso" confirma que la vulnerabilidad existe.
 */
public class SecuritySteps {

    /* ------------------------------------------------------------------ */
    /* UC-09: SQL Injection que bypassea la autenticacion                  */
    /* ------------------------------------------------------------------ */

    @Dado("que el atacante está en la página de inicio de sesión")
    public void atacanteEnLogin() {
        Ui.open("/#/login");
        Ui.dismissBanners();
        Ui.visible(Ui.EMAIL_FIELD);
    }

    @Cuando("inicia sesión con la inyección SQL {string} y la contraseña {string}")
    public void loginConInyeccion(String inyeccion, String password) {
        org.openqa.selenium.WebElement email = Ui.visible(Ui.EMAIL_FIELD);
        email.clear();
        email.sendKeys(inyeccion);
        org.openqa.selenium.WebElement pass = Ui.visible(Ui.PASSWORD_FIELD);
        pass.clear();
        pass.sendKeys(password);
        pass.sendKeys(org.openqa.selenium.Keys.TAB);
        Ui.clickable(Ui.LOGIN_BUTTON).click();
        Ui.sleep(1500);
    }

    /**
     * Confirmacion de la vulnerabilidad: se autentica sin credenciales validas
     * (el test Verde = vulnerabilidad presente en la app objetivo).
     */
    @Entonces("la autenticación se acepta sin credenciales válidas")
    public void autenticadoSinCredenciales() {
        Ui.sleep(500);
        new org.openqa.selenium.support.ui.WebDriverWait(Ui.driver(), Duration.ofSeconds(20))
                .until(d -> {
                    Ui.clickIfPresent(Ui.ACCOUNT_MENU);
                    return Ui.isDisplayed(Ui.LOGOUT_BUTTON);
                });
        assertTrue(Ui.isDisplayed(Ui.LOGOUT_BUTTON),
                "No se bypassó la autenticación con SQL injection (la app estaría protegida)");
    }

    /* ------------------------------------------------------------------ */
    /* UC-10: XSS reflejado en el buscador                                 */
    /* ------------------------------------------------------------------ */

    @Dado("que el atacante abre el buscador")
    public void atacanteAbreBuscador() {
        Ui.open("/");
        Ui.dismissBanners();
        Ui.clickable(Ui.SEARCH_TOGGLE);
    }

    @Cuando("inyecta la carga útil")
    public void inyectaPayload(String payload) {
        WebElement input = Ui.visible(Ui.SEARCH_INPUT);
        input.click();
        input.clear();
        for (char c : payload.toCharArray()) {
            input.sendKeys(String.valueOf(c));
            Ui.sleep(40);
        }
        Ui.sleep(300);
        input.sendKeys(org.openqa.selenium.Keys.ENTER);
        Ui.sleep(1500);
    }

    /**
     * Confirmacion de la vulnerabilidad: el iframe inyectado ejecuta
     * javascript:alert() y Selenium detecta la ventana de alerta.
     */
    @Entonces("la aplicación ejecuta el script y abre una ventana de alerta")
    public void alertaXss() {
        Optional<Alert> alerta = Ui.waitForAlert(Duration.ofSeconds(10));
        assertTrue(alerta.isPresent(),
                "No se detectó alerta JS: la app no reflejaría el XSS (app protegida)");
        alerta.get().accept(); // cierra la alerta para liberar la sesión del browser
        System.out.println("[seguridad] XSS reflejado confirmado: la alerta se desplegó");
    }
}
