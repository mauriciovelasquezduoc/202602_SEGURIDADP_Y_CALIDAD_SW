package cl.duoc.lab.bdd.steps;

import cl.duoc.lab.bdd.support.Ui;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.openqa.selenium.Keys;

import static cl.duoc.lab.bdd.support.Ui.ACCOUNT_MENU;
import static cl.duoc.lab.bdd.support.Ui.EMAIL_FIELD;
import static cl.duoc.lab.bdd.support.Ui.LOGIN_BUTTON;
import static cl.duoc.lab.bdd.support.Ui.LOGOUT_BUTTON;
import static cl.duoc.lab.bdd.support.Ui.PASSWORD_FIELD;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pasos de autenticacion: login valido, invalido y verificacion de sesion.
 */
public class LoginSteps {

    @Dado("el usuario está autenticado con {string} y {string}")
    public void usuarioAutenticado(String email, String password) {
        Ui.open("/#/login");
        Ui.dismissBanners();
        iniciarSesion(email, password);
        verSesionIniciada();
    }

    @Cuando("inicia sesión con {string} y {string}")
    public void iniciaSesion(String email, String password) {
        iniciarSesion(email, password);
    }

    @Entonces("la sesión queda iniciada")
    public void verSesionIniciada() {
        Ui.sleep(500);
        Ui.clickable(ACCOUNT_MENU);
        // abrir el menu de usuario: aparece la opcion de cerrar sesion
        new org.openqa.selenium.support.ui.WebDriverWait(Ui.driver(), java.time.Duration.ofSeconds(20))
                .until(d -> {
                    Ui.clickIfPresent(ACCOUNT_MENU);
                    return Ui.isDisplayed(LOGOUT_BUTTON);
                });
        assertTrue(Ui.isDisplayed(LOGOUT_BUTTON),
                "El menú de usuario no muestra la opción de cierre de sesión");
    }

    @Entonces("el menú de usuario muestra la cuenta")
    public void elMenuMuestraLaCuenta() {
        // reutiliza la comprobacion robusta (boton logout visible en el menu)
        verSesionIniciada();
    }

    @Entonces("la sesión no queda iniciada")
    public void verSesionNoIniciada() {
        Ui.sleep(2000);
        assertTrue(Ui.driver().getCurrentUrl().contains("#/login"),
                "Tras el intento fallido se esperaba seguir en la página de login, url: "
                        + Ui.driver().getCurrentUrl());
        assertFalse(Ui.isDisplayed(LOGOUT_BUTTON),
                "No debería existir opción de cierre de sesión");
    }

    @Entonces("se muestra el mensaje {string}")
    public void mensajeDeError(String esperado) {
        Ui.sleep(1000);
        assertTrue(Ui.textoVisible(esperado),
                "No se encontró el mensaje: " + esperado);
    }

    @Entonces("se muestra un mensaje de error")
    public void mensajeErrorGenerico() {
        Ui.sleep(1000);
        assertTrue(Ui.textoVisible("Invalid email or password"),
                "No se encontró el mensaje de credenciales inválidas en la página");
    }

    static void iniciarSesion(String email, String password) {
        Ui.visible(EMAIL_FIELD).click();
        org.openqa.selenium.WebElement emailEl = Ui.visible(EMAIL_FIELD);
        emailEl.clear();
        emailEl.sendKeys(email);
        org.openqa.selenium.WebElement passEl = Ui.visible(PASSWORD_FIELD);
        passEl.clear();
        passEl.sendKeys(password);
        passEl.sendKeys(Keys.TAB); // fuerza el blur de los campos antes de enviar
        Ui.clickable(LOGIN_BUTTON).click();
        Ui.sleep(1500);
    }
}
