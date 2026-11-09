package cl.duoc.lab.bdd.steps;

import cl.duoc.lab.bdd.support.Ui;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;

import static cl.duoc.lab.bdd.support.Ui.PRODUCT_NAME;
import static cl.duoc.lab.bdd.support.Ui.RESULT_NAME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pasos del buscador de productos.
 */
public class SearchSteps {

    @Cuando("busca {string}")
    public void busca(String termino) {
        Ui.buscar(termino);
    }

    @Entonces("se muestran al menos {int} resultados")
    public void seMuestranResultados(int minimo) {
        assertTrue(Ui.count(RESULT_NAME) >= minimo,
                "Se esperaban al menos " + minimo + " resultados de búsqueda");
    }

    @Entonces("los resultados contienen {string} en el nombre")
    public void losResultadosContienen(String texto) {
        boolean coincide = Ui.driver().findElements(PRODUCT_NAME).stream()
                .limit(20)
                .anyMatch(el -> el.getText().toLowerCase().contains(texto.toLowerCase()));
        assertTrue(coincide,
                "Ningun resultado visible contiene '" + texto + "' en el nombre");
    }

    @Entonces("no se muestra ningún producto como resultado")
    public void noSeMuestraNingunProducto() {
        Ui.sleep(1500);
        assertFalse(Ui.count(RESULT_NAME) > 0,
                "La búsqueda sin coincidencias no debería listar productos");
    }
}
