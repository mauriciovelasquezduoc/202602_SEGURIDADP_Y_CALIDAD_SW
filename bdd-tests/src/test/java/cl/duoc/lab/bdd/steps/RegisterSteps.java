package cl.duoc.lab.bdd.steps;

import cl.duoc.lab.bdd.support.Ui;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;

import static cl.duoc.lab.bdd.support.Ui.REG_BUTTON;
import static cl.duoc.lab.bdd.support.Ui.REG_EMAIL;
import static cl.duoc.lab.bdd.support.Ui.REG_PASSWORD;
import static cl.duoc.lab.bdd.support.Ui.REG_REPEAT;
import static cl.duoc.lab.bdd.support.Ui.REG_SEC_ANS;
import static cl.duoc.lab.bdd.support.Ui.REG_SEC_OPT;
import static cl.duoc.lab.bdd.support.Ui.REG_SEC_Q;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pasos del registro de usuarios nuevos.
 */
public class RegisterSteps {

    /**
     * Registra un usuario con email unico generado por timestamp, una
     * contrasena fuerte y una respuesta de seguridad cualquiera.
     * La pregunta de seguridad es un mat-select de Angular Material: se abre
     * con click y se elige la primera opcion disponible del overlay.
     */
    @Cuando("registra un nuevo usuario con respuesta de seguridad {string}")
    public void registraNuevoUsuario(String respuestaSeguridad) {
        String email = "qa." + System.currentTimeMillis() + "@lab.duoc.cl";
        String password = "S3gur1d4d#Lab";

        Ui.visible(REG_EMAIL).sendKeys(email);
        Ui.visible(REG_PASSWORD).sendKeys(password);
        Ui.visible(REG_REPEAT).sendKeys(password);

        // dropdown de pregunta de seguridad
        Ui.clickable(REG_SEC_Q).click();
        Ui.sleep(800);
        Ui.first(REG_SEC_OPT).click(); // answers are static labels, any works
        Ui.sleep(800);

        Ui.visible(REG_SEC_ANS).sendKeys(respuestaSeguridad + " " + System.currentTimeMillis());
        Ui.clickable(REG_BUTTON).click();
        Ui.sleep(2000);
    }

    @Entonces("el registro se completa con un mensaje de éxito")
    public void registroExitoso() {
        assertTrue(Ui.snackMuestra("successfully")
                        || Ui.snackMuestra("Registration completed"),
                "No se mostró el mensaje 'Registration completed successfully'");
    }

    @Entonces("la contraseña débil es rechazada")
    public void contrasenaRechazada() {
        // juice shop marca la fuerza de la contraseña en la barra de progreso:
        // rechazo valido = la pagina NO muestra el snack bar de exito
        assertTrue(!Ui.snackMuestra("successfully"),
                "Una contraseña débil no debería ser aceptada");
    }
}
