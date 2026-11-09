package cl.duoc.lab.bdd.hooks;

import cl.duoc.lab.bdd.support.Ui;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Hooks de ciclo de vida de cada escenario:
 *  - captura screenshot de evidencia (paso o fallo) hacia /evidencias
 *  - adjunta el screenshot al reporte HTML de Cucumber
 *  - cierra el navegador para partir cada escenario limpio
 */
public class Hooks {

    @After
    public void capturarEvidencia(Scenario scenario) {
        try {
            WebDriver driver = Ui.current();
            if (driver instanceof TakesScreenshot ts) {
                byte[] screenshot = ts.getScreenshotAs(OutputType.BYTES);
                scenario.attach(screenshot, "image/png", "evidencia final");
                guardarArchivo(scenario, screenshot);
            }
        } catch (Exception e) {
            System.err.println("[evidencia] no se pudo capturar: " + e.getMessage());
        } finally {
            Ui.quit();
        }
    }

    private void guardarArchivo(Scenario scenario, byte[] bytes) {
        try {
            String tag = scenario.getSourceTagNames().stream()
                    .filter(t -> t.startsWith("@uc"))
                    .findFirst()
                    .orElse("@uc");
            String nombre = tag.substring(1)
                    + "_" + slug(scenario.getName())
                    + "_" + (scenario.isFailed() ? "FALLA" : "OK")
                    + ".png";
            Path dir = Path.of(cl.duoc.lab.bdd.config.TestConfig.EVIDENCIAS_DIR);
            Files.createDirectories(dir);
            Files.write(dir.resolve(nombre), bytes);
            System.out.println("[evidencia] screenshot: " + dir.resolve(nombre)
                    + " -> " + List.of(scenario.getStatus().toString()));
        } catch (Exception e) {
            System.err.println("[evidencia] no se pudo guardar archivo: " + e.getMessage());
        }
    }

    private String slug(String texto) {
        return texto == null ? "escenario"
                : texto.replaceAll("[^a-zA-Z0-9\\-_ ]", "")
                       .replace(' ', '_')
                       .toLowerCase();
    }
}
