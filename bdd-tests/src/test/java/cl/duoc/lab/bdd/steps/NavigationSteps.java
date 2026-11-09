package cl.duoc.lab.bdd.steps;

import cl.duoc.lab.bdd.support.Ui;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pasos comunes de navegacion y del catalogo de productos.
 */
public class NavigationSteps {

    @Dado("que el usuario accede al sitio Juice Shop")
    public void queElUsuarioAccedeAlSitio() {
        Ui.open("/");
        Ui.dismissBanners();
        Ui.visible(Ui.PRODUCT_CARD);
    }

    @Dado("que el usuario está en la página de inicio de sesión")
    public void paginaLogin() {
        Ui.open("/#/login");
        Ui.dismissBanners();
        Ui.visible(Ui.LOGIN_BUTTON);
    }

    @Dado("que el usuario accede a la página de registro")
    public void paginaRegistro() {
        Ui.open("/#/register");
        Ui.dismissBanners();
    }

    @Entonces("el título de la página es {string}")
    public void elTituloDeLaPaginaEs(String esperado) {
        assertEquals(esperado, Ui.driver().getTitle(),
                "El <title> del sitio no coincide");
    }

    @Entonces("el catálogo muestra al menos {int} productos con nombre y precio")
    public void elCatalogoMuestraProductos(int minimo) {
        assertTrue(Ui.count(Ui.PRODUCT_NAME) >= minimo,
                "Se esperaban al menos " + minimo + " productos en la grilla");
        assertTrue(Ui.count(Ui.PRODUCT_PRICE) >= minimo,
                "Se esperaban al menos " + minimo + " precios visibles");
    }
}
