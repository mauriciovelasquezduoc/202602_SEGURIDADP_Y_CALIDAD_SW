# PASO A PASO — De una feature en Gherkin al reporte con evidencias
## Lab S14: Cucumber + Selenium + OWASP ZAP sobre Juice Shop

Este documento explica la cadena completa **end-to-end** usando casos reales del
lab: cómo se escribe una feature, cómo se programa en Java, cómo entra Selenium
en escena, cómo se ejecuta todo en Docker, y qué reportes y evidencias quedan.

---

## 0. El flujo en una imagen

```
 (1) FEATURE (Gherkin)              src/test/resources/features/*.feature
        │   Cucumber lee las frases en lenguaje natural
        ▼
 (2) STEP DEFINITIONS (Java)        cl.duoc.lab.bdd.steps.*
        │   cada frase => @Dado/@Cuando/@Entonces => acciones
        ▼
 (3) SELENIUM (WebDriver)           RemoteWebDriver -> selenium:4444 (Chromium)
        │   el navegador navega POR el proxy de ZAP
        ▼
 (4) APP OBJETIVO                      Juice Shop (juice-shop:3000)
        │
        ├─ (5) HOOK @After            screenshot -> evidencias/*.png
        ├─ (6) PLUGINS Cucumber       target/cucumber-report/*.json|html
        └─ (7) HOOK @AfterAll (ZAP)   spider + active scan -> evidencias/zap-report.html
```

---

## 1. Escribir la feature (Gherkin)

Las features viven en `bdd-tests/src/test/resources/features/` y describen el
caso de uso **sin código**. Tres reglas del lab:

1. La **primera línea** declara el idioma: `# language: es` (keywords en
   español: `Característica`, `Escenario`, `Dado`, `Cuando`, `Entonces`, `Y`).
2. El primer `@tag` debe ser `@ucNN` — es lo que usa el hook para nombrar la
   evidencia PNG.
3. `@funcional` o `@seguridad` permiten filtrar corridas (ver paso 4).

Ejemplo real — `uc07_carrito_anadir.feature`:

```gherkin
# language: es
@uc07 @funcional
Característica: Añadir productos al carrito
  Como cliente del sitio
  quiero añadir productos al carrito
  para poder pagarlos después

  Escenario: Añadir un producto incrementa el carrito en 1
    Dado que el usuario accede al sitio Juice Shop
    Cuando añade "Apple Juice (1000ml)" al carrito
    Entonces el contador del carrito refleja la cantidad añadida
    Y abre el carrito
    Y el carrito contiene "Apple Juice (1000ml)"
```

Puntos didácticos:

- **Característica / Escenario**: el "qué" del caso de uso (relación + comportamiento observable).
- **Dado**: precondición. **Cuando**: acción del usuario. **Entonces/Y**: resultados observables.
- Las comillas `"..."` delimitan **parámetros** del paso (Cucumber los entrega
  como argumentos al método Java del paso 2).
- Tip práctico: no dejes ni tabuladores raros ni keywords en inglés mezcladas
  con el header `# language: es`; si el parser no reconoce la keyword la
  corrida aborta en discovery ("Failed to parse resource").

---

## 2. Programar los pasos (step definitions)

Cada frase de la feature debe casar EXACTAMENTE (mismo verbo, mismos espacios)
con una anotación de una clase Java llamada "step definition". Viven en
`bdd-tests/src/test/java/cl/duoc/lab/bdd/steps/`.

El enlace Cucumber lo resuelve **por texto**, no por nombre de método:

| Frase en la feature                        | Anotación Java                                   |
|--------------------------------------------|--------------------------------------------------|
| `Dado que el usuario accede al sitio...`   | `@Dado("que el usuario accede al sitio Juice Shop")` |
| `Cuando añade "X" al carrito`              | `@Y("añade {string} al carrito")` con parámetro `String producto` |
| `Entonces se muestran al menos 2 resultados` | `@Entonces("se muestran al menos {int} resultados")` con `int` |

Parámetros disponibles: `{string}`, `{int}`, `{float}`, tablas de datos
(`Ejemplos:` de un Scenario Outline) y DocStrings (bloques `"""` — véase UC-10).

Ejemplo real (`CartSteps.java`, paso "añade al carrito"):

```java
public class CartSteps {

    private int contadorInicial;

    @Y("añade {string} al carrito")
    public void anadeAlCarrito(String producto) {
        contadorInicial = leerContador();              // estado local del escenario

        // 1) localiza la tarjeta del producto por su texto visible
        WebElement tarjeta = Ui.first(
                By.xpath("//app-product[contains(., '" + producto + "')]"));

        // 2) dentro de esa tarjeta, el botón "Add to Basket"
        tarjeta.findElement(Ui.ADD_BUTTON).click();

        // 3) espera EXPLÍCITA: el badge del carrito debe pasar de N a N+1
        new WebDriverWait(Ui.driver(), Duration.ofSeconds(20))
                .until(d -> leerContador() == contadorInicial + 1);
    }
}
```

> **Regla de oro BDD**: los steps expresan *intención de negocio* ("añadir al
> carrito"), nunca detalles como `click en cssSelector .btn-basket`. Esos
> detalles viven en la clase `Ui` (paso 3). Así la feature sigue legible para
> un profesor/cliente y cambiar un selector no obliga a reescribir Gherkin.

---

## 3. El enlace con Selenium

Los steps no manejan navegadores directamente: mandan a la clase
`bdd-tests/src/test/java/cl/duoc/lab/bdd/support/Ui.java`, que administra:

**a) Un WebDriver por escenario** (`Ui.driver()` / `Ui.quit()` en el hook).
El `docker-compose.yml` expone el servicio `selenium` (Chromium headless en
`http://selenium:4444/wd/hub`) y el tests lo usa vía `RemoteWebDriver`:

```java
ChromeOptions options = new ChromeOptions();
options.addArguments("--no-sandbox", "--disable-dev-shm-usage",
                     "--ignore-certificate-errors");   // cert de ZAP
Proxy zap = new Proxy();                                // el tráfico pasa por ZAP
zap.setHttpProxy("http://zap:8080").setSslProxy("http://zap:8080");
options.setProxy(zap);
return new RemoteWebDriver(new URL(TestConfig.SELENIUM_URL), options);
```

**b) Navegación + esperas explícitas** (la app es una SPA Angular; nunca uses
`Thread.sleep` a ciegas ni `findElement` directo):

```java
Ui.open("/#/login");               // get() + espera document.readyState=complete
Ui.visible(By.id("email")).sendKeys("admin@juice-sh.op");
Ui.clickable(By.id("loginButton")).click();  // visible AND enabled
Ui.isDisplayed(By.id("navbarLogoutButton")); // poll 500ms hasta 20s
```

**c) Localizadores calibrados contra el DOM real** de esta versión de Juice
Shop (los ves centralizados como constantes en `Ui`):

```
PRODUCT_CARD  = app-product                     (grilla de productos)
PRODUCT_NAME  = app-product .name               (nombre del producto)
ADD_BUTTON    = button[aria-label='Add to Basket']
CART_COUNTER  = button[aria-label='Show the shopping cart'] .fa-layers-counter
CART_ROWS     = mat-row        /  CART_CELLS = mat-cell
CART_TOTAL    = #price         ("Total Price: 1.99¤")
SEARCH_TOGGLE = #searchQuery .search-toggle      (lupa del navbar)
SEARCH_INPUT  = #searchQuery .search-container input
```

> ¿Cambia el markup de Juice Shop y todo deja de pasar? Existe una **sonda**
> (`cl.duoc.lab.bdd.Probe`, clase con `main`, no es un test) que abre las
> páginas clave, las recorre y deja el DOM real en `evidencias/dom-*.html`
> para recalibrar selectores sin adivinar:
>
> ```bash
> docker compose run --rm -e ZAP_API_URL= -e ZAP_ACTIVE_SCAN=false tests \
>   mvn -q test-compile org.codehaus.mojo:exec-maven-plugin:3.1.0:java \
>   -Dexec.mainClass=cl.duoc.lab.bdd.Probe -Dexec.classpathScope=test
> ```

---

## 4. Ejecutar

### 4.1 Cómo Cucumber descubre todo

- `RunCucumberTest.java` anota `@Suite @IncludeEngines("cucumber")
  @SelectClasspathResource("features")` → JUnit 5 arranca el engine de Cucumber.
- `junit-platform.properties` fija `cucumber.glue=cl.duoc.lab.bdd` → ahí
  busca `@Dado/@Cuando/@Entonces` y los `hooks`.
- Maven Surefire está configurado (pom.xml) para correr ese runner:
  `mvn test`.

### 4.2 Corrida completa en Docker (la forma normal)

```bash
cd lab
docker compose up -d            # levanta juice-shop + zap + selenium (y dispara tests)
docker compose run --rm tests   # re-ejecutable a demanda
```

- Primera vez Maven baja dependencias (~1-2 min).
- Cada escenario imprime su estado; al final: `11 Scenarios (11 passed)`
  (10 UC; UC-05 usa `Ejemplos` con 2 filas → 11 ejecuciones).
- El hook `@AfterAll` ejecuta el DAST de ZAP y escribe sus reportes.

### 4.3 Variantes útiles

```bash
# solo funcional, sin active scan (rápido, ~1 min)
docker compose run --rm -e ZAP_ACTIVE_SCAN=false tests mvn test -Dcucumber.filter.tags=@funcional

# solo seguridad
docker compose run --rm tests mvn test -Dcucumber.filter.tags=@seguridad

# un solo UC
docker compose run --rm tests mvn test -Dcucumber.filter.tags=@uc09

# validar EL ENLACE feature<->steps sin abrir navegador (dry run)
mvn test -Dcucumber.execution.dry-run=true        # local: "Tests run: 11"
```

> El **dry run** es la mejor manera de verificar el paso 2: si una frase no
> encuentra su método Java o hay ambigüedades, la ignoración o el error salen ahí.

---

## 5. Cómo queda el reporte

### 5.1 Reporte Cucumber HTML (el entregable principal)

Los plugins están en `bdd-tests/src/test/resources/cucumber.properties`:

```properties
cucumber.plugin=pretty, json:target/cucumber-report/cucumber.json, \
  html:target/cucumber-report/cucumber.html, junit:target/cucumber-report/cucumber-xml.xml
```

Tras la corrida, abre:

```bash
open lab/bdd-tests/target/cucumber-report/cucumber.html
```

Qué contiene:
- Lista de features → escenarios → pasos, con **verde/rojo por paso**.
- **Screenshots embebidos** en cada escenario (el hook adjunta el PNG con
  `scenario.attach(...)`), incluida la página exacta en que falló.
- Error/assert message por paso (la evidencia textual del fallo).
- Botón para alternar entre Vista "Features" o "Tags" (`@seguridad`, `@funcional`).

### 5.2 Reportes maquina-legibles

| Archivo | Uso típico |
|---|---|
| `target/cucumber-report/cucumber.json` | subir a CI (GitHub Action, Jenkins) |
| `target/cucumber-report/cucumber-xml.xml` | formato JUnit para dashboards |
| `target/surefire-reports/TEST-*.xml` | estándar de Maven (Jenkins/GitLab) |

### 5.3 Reporte DAST de OWASP ZAP

Al terminar la suite (hook `@AfterAll` en `ZapHook.java`), en `evidencias/`:

1. `zap-report.html` — reporte HTML con todas las alertas (riesgo alto/medio/bajo).
2. `zap-alerts.json` — JSON crudo de `/JSON/core/view/alerts/`.

Aquí aporta el doble flujo: Selenium ejercitó la app (escaneo **pasivo** de
ZAP sobre cada request) + spider y active scan contra `juice-shop:3000`.

---

## 6. Cómo quedan las evidencias

El hook `cl.duoc.lab.bdd.hooks.Hooks` (`@After`) captura un screenshot al
terminar **cada escenario** (pase o falla) y lo guarda:

```
evidencias/
├── uc01_..._OK.png
├── uc04_el_login_vlido_autentica_al_usuario_demo_OK.png       (paso)
├── uc09_la_tautologa__or_11--_autentica_a_un_atacante_OK.png  (hallazgo SQLi!)
├── ...
├── dom-home.html        (País si corriste la sonda Probe)
├── zap-report.html      (Reporte DAST de ZAP)
└── zap-alerts.json
```

Reglas de nomenclatura: `<tag><escenario slugged>_<OK|FALLA>.png`.
El mismo PNG queda embebido en el `cucumber.html` (no hay que adjuntarlo
manualmente).

> Nota: si limpias antes de cada entrega: `rm evidencias/*.png` para que no se
> acumulen corridas viejas.

---

## 7. Resumen: qué toca tocar cuando algo falla

| Síntoma en el reporte | Causa | Dónde se arregla |
|---|---|---|
| `NoSuchElement` / timeout en un paso | selector obsoleto (cambió el DOM) | constantes en `Ui.java` (calibrar con `Probe`) |
| `Ambiguous step` | dos methods matchean la misma frase | annotations más específicas en `steps/` |
| `Undefined step` (dry run) | falta programar la frase | nueva anotación en una clase de `steps/` |
| `Failed to parse resource` (Gherkin) | keyword desconocida / idioma incorrecto | el `.feature` (header `# language: es`) |
| `Connection refused` a selenium | node no levantó | `docker compose ps` y reintentar |
| Escena pasa pero en ZAP no hay alertas | proxy no configurado en el Chrome | `TestConfig.ZAP_PROXY` / compose |
| El reporte ZAP no se genera | API key o URL de ZAP mal | `ZAP_API_URL` / `ZAP_API_KEY` en compose |
