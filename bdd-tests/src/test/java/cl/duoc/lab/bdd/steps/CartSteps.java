package cl.duoc.lab.bdd.steps;

import cl.duoc.lab.bdd.support.Ui;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pasos del carrito de compras: agregar, abrir, cambiar cantidad y validar
 * el recalculo del total.
 *
 * DOM real del lab: la fila es <mat-row> con celdas
 *   [imagen] [producto .mat-column-product] [cantidad con botones +/-]
 *   [precio .mat-column-price] [boton basura .mat-column-remove]
 * y el total vive en <div id="price"> ("Total Price: 1.99¤").
 */
public class CartSteps {

    private int contadorInicial;
    private double totalAntes;
    private int cantidadAntes;

    @Y("añade {string} al carrito")
    public void anadeAlCarrito(String producto) {
        contadorInicial = leerContador();

        // tarjeta del producto buscada por su texto visible
        WebElement tarjeta = Ui.first(
                By.xpath("//app-product[contains(., '" + producto + "')]"),
                By.xpath("//mat-card[contains(., '" + producto + "')]"));
        tarjeta.findElement(Ui.ADD_BUTTON).click();

        // esperar a que el badge del carrito incremente
        new org.openqa.selenium.support.ui.WebDriverWait(Ui.driver(), java.time.Duration.ofSeconds(20))
                .until(d -> leerContador() == contadorInicial + 1);
    }

    @Y("abre el carrito")
    public void abreElCarrito() {
        Ui.clickable(Ui.CART_BUTTON).click();
        Ui.sleep(1500);
        Ui.visible(Ui.CART_TOTAL); // "Total Price: ..."
        Ui.sleep(1000);
    }

    @Cuando("cambia la cantidad de la primera fila a {int}")
    public void cambiaCantidad(int objetivo) {
        WebElement fila = Ui.driver().findElements(Ui.CART_ROWS).get(0);
        WebElement celdaCantidad = fila.findElement(By.cssSelector("mat-cell.mat-column-quantity"));

        // la cantidad vive en el span "cell-initial-font" de esa celda
        cantidadAntes = Integer.parseInt(
                celdaCantidad.findElement(By.cssSelector("span.cell-initial-font"))
                        .getText().trim());
        totalAntes = extraerMonto(Ui.visible(Ui.CART_TOTAL).getText());

        WebElement botonMas = celdaCantidad.findElements(By.tagName("button")).get(1);
        while (cantidadAntes < objetivo) {
            botonMas.click();
            cantidadAntes++;
            Ui.sleep(1200);
        }
        Ui.sleep(1500); // el total se recalcula asincronicamente
    }

    @Entonces("el contador del carrito refleja la cantidad añadida")
    public void elContadorRefleja() {
        assertEquals(contadorInicial + 1, leerContador(),
                "El badge del carrito no incrementó al añadir el producto");
    }

    @Entonces("el carrito contiene {string}")
    public void elCarritoContiene(String producto) {
        List<WebElement> filas = Ui.driver().findElements(Ui.CART_ROWS);
        assertTrue(!filas.isEmpty(), "El carrito no muestra filas");
        boolean contiene = filas.stream()
                .flatMap(f -> f.findElements(By.cssSelector("mat-cell.mat-column-product")).stream())
                .anyMatch(c -> c.getText().contains(producto));
        assertTrue(contiene, "El carrito no lista el producto '" + producto + "'");
    }

    @Entonces("el subtotal corresponde al precio por la cantidad")
    public void elSubtotalCorresponde() {
        double esperado = totalAntes / cantidadAntes * 2.0; // precio unitario * cantidad objetivo (2)
        double despues = extraerMonto(Ui.visible(Ui.CART_TOTAL).getText());

        // la cantidad final es 2 (1 -> 2 con el boton "+")
        assertEquals(esperado, despues, 0.01,
                "El total no se recalculó como precio unitario * 2");
    }

    /* --------------------------- utilidades ---------------------------- */

    private int leerContador() {
        try {
            List<WebElement> contadores = Ui.driver().findElements(Ui.CART_COUNTER);
            if (contadores.isEmpty()) return 0;
            return Integer.parseInt(contadores.get(0).getText().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    /** Extrae el primer monto decimal de un texto (ej: "Total Price: 1.99¤"). */
    private double extraerMonto(String texto) {
        Matcher m = Pattern.compile("([0-9]+\\.[0-9]{1,2})").matcher(texto);
        if (!m.find()) {
            throw new AssertionError("No se encontró un monto en: " + texto);
        }
        return Double.parseDouble(m.group(1));
    }
}
