# GUÍA LAB S14 — BDD (Cucumber/Gherkin) + Selenium + OWASP ZAP sobre Juice Shop

## 1. Qué se construyó

| # | Caso de uso | Tipo | Archivo feature |
|---|-------------|------|-----------------|
| UC-01 | Visualizar catálogo de productos | Funcional | `uc01_catalogo.feature` |
| UC-02 | Buscar productos por nombre | Funcional | `uc02_busqueda.feature` |
| UC-03 | Búsqueda sin resultados | Funcional | `uc03_busqueda_vacia.feature` |
| UC-04 | Login con credenciales válidas | Funcional | `uc04_login_valido.feature` |
| UC-05 | Rechazo de credenciales inválidas | Funcional | `uc05_login_invalido.feature` |
| UC-06 | Registro de nuevo usuario | Funcional | `uc06_registro.feature` |
| UC-07 | Añadir producto al carrito | Funcional | `uc07_carrito_anadir.feature` |
| UC-08 | Cambiar cantidad y recalcular subtotal | Funcional | `uc08_carrito_cantidad.feature` |
| UC-09 | Bypass de login con SQL Injection | Seguridad | `uc09_sqli_login.feature` |
| UC-10 | XSS reflejado en el buscador | Seguridad | `uc10_xss_busqueda.feature` |

> Nota didáctica: UC-09 y UC-10 "prueban que la vulnerabilidad EXISTE" en Juice Shop
> (app con fallos intencionales). Un test verde = hallazgo de seguridad confirmado
> con evidencia de Selenium. Si el test falla, la app estaría protegida.

Pipeline de la prueba (arquitectura del lab):

```
   [tests Java] --(RemoteWebDriver)--> [Selenium node: Chromium] --(proxy)--> [ZAP daemon] --> [Juice Shop]
        |                          |
        |-- screenshots ---------->| evidencias/*.png
        |-- cucumber-json/html --->| bdd-tests/target/cucumber-report/
        |-- reporte ZAP (hook @AfterAll) -->| evidencias/zap-report.html
```

- **Gherkin**: los 10 casos de uso están escritos en lenguaje natural (`features/`).
- **Cucumber** los ejecuta vía JUnit 5 (runner `cl.duoc.lab.bdd.RunCucumberTest`).
- **Selenium** automatiza el navegador (RemoteWebDriver contra el node Chromium).
- **OWASP ZAP** hace el DAST: escaneo pasivo de todo el tráfico de los tests +
  spider + active scan al final, y emite su propio reporte.

## 2. Estructura del proyecto

```
lab/
├── docker-compose.yml          # 4 servicios: juice-shop, zap, selenium, tests
├── evidencias/                 # screenshots por escenario + reportes ZAP
├── salida/                     # esta guía y documentos generados
└── bdd-tests/
    ├── pom.xml                 # Maven: Cucumber 7 + Selenium 4 + JUnit 5
    └── src/test/
        ├── java/cl/duoc/lab/bdd/
        │   ├── RunCucumberTest.java      # runner
        │   ├── config/TestConfig.java    # variables de entorno
        │   ├── support/Ui.java           # WebDriver + esperas + localizadores
        │   ├── steps/                    # paso a paso Java (glue)
        │   │   ├── NavigationSteps.java
        │   │   ├── SearchSteps.java
        │   │   ├── LoginSteps.java
        │   │   ├── RegisterSteps.java
        │   │   ├── CartSteps.java
        │   │   └── SecuritySteps.java
        │   └── hooks/
        │       ├── Hooks.java            # screenshot por escenario
        │       └── ZapHook.java          # spider + active scan + reporte ZAP
        └── resources/
            ├── features/*.feature        # los 10 casos de uso en Gherkin
            ├── cucumber.properties       # plugins de reporte
            └── junit-platform.properties # glue del engine (evita warnings)
```

## 3. Paso a paso de ejecución

### Paso 0 — Prerrequisitos
- Docker Desktop corriendo (el lab no requiere Java local).
- Puertos 3000, 8090, 7900 libres.

### Paso 1 — Levantar el entorno ( ~3-5 min la primera vez)

```bash
cd lab
docker compose up -d
docker compose ps
```

Verifica que `lab-juice-shop` esté `healthy`, `lab-zap` `healthy` y
`lab-selenium` `Up`. Puedes explorar la app manualmente en
`http://localhost:3000` mientras tanto.

- Para "mirar" cómo interactúa Selenium con la app: VNC en
  `http://localhost:7900` (passphrase `secret`, por defecto del contenedor).

### Paso 2 — Ejecutar la suite BDD

Primera vez: `docker compose up -d` ya ejecuta el servicio `tests`.
Para volver a correr (o correr a demanda):

```bash
docker compose run --rm tests
```

Verás en consola:
1. La descarga de dependencias Maven la primera vez (~1-2 min).
2. Cada escenario con su estado (pasado/fallado), paso a paso.
3. Un resumen tipo:
   `11 Scenarios (11 passed)` (10 casos de uso; UC-05 usa un Scenario Outline
   con 2 ejemplos, por eso son 11 ejecuciones).
4. Al final los logs `[ZAP] ...` del hook `@AfterAll` que ejecuta el DAST.

> **Atención**: con `ZAP_ACTIVE_SCAN=true` (default del compose) el Active Scan
> puede tardar 10-20 min. Para una iteración rápida, desactívalo:
>
> ```bash
> docker compose run --rm -e ZAP_ACTIVE_SCAN=false tests
> ```

### Paso 3 — Reportes Cucumber

Después de cada corrida quedan en `lab/bdd-tests/target/`:

| Archivo | Qué es |
|---------|--------|
| `cucumber-report/cucumber.html` | **Reporte HTML principal** (ábrela en el navegador) |
| `cucumber-report/cucumber.json` | JSON estándar (para CI o herramientas) |
| `cucumber-report/cucumber-xml.xml` | XML formato JUnit (para Jenkins/GitLab) |
| `surefire-reports/` | XML de Maven Surefire por feature |

Abrir el reporte:

```bash
open lab/bdd-tests/target/cucumber-report/cucumber.html   # macOS
```

El reporte HTML incluye el `pretty` por consola + los screenshots adjuntados.

### Paso 4 — Evidencias de pruebas

En `lab/evidencias/` quedará un PNG **por escenario** con el resultado:

```
uc01_..._paso_final_OK.png
uc09_..._paso_final_FILAD.png
```

(El nombre incluye el tag del UC, el escenario y el estado `OK`/`FILA` → `FALLA`.)

Y los reportes del motor DAST (generados al final de la suite por `ZapHook`):

| Archivo | Qué es |
|---------|--------|
| `zap-report.html` | Reporte HTML de ZAP con las vulnerabilidades detectadas |
| `zap-alerts.json` | JSON crudo con todas las alertas (pasivas + activas) |

### Paso 5 — Consultar ZAP manualmente (opcional)

Mientras el entorno está levantado, la API de ZAP está en el host:
`http://localhost:8090` (requiere `api key` = `zap-key-lab-s14`).
Ejemplo: `curl "http://localhost:8090/JSON/core/view/alerts/?apikey=zap-key-lab-s14"`.

### Paso 6 — Bajar entorno

```bash
docker compose down -v
```

> `-v` borra también la sesión persistente de ZAP (volumen `zap-sessions`), con lo
> que la próxima corrida empieza con un historial de alertas limpio.

## 4. Cómo armar el reporte final del laboratorio (entregable)

Contenido sugerido para el entregable en PDF/documento:

1. **Tabla de resumen BDD**: para cada UC (1-10) registrar `OK/FALLA`,
   ir a `cucumber-report/cucumber.html` y screenshot del escenario.
2. **Evidencias por escenario**: adjuntar el PNG de `evidencias/` correspondiente.
3. **Hallazgos de seguridad**: para UC-09 y UC-10, describir la vulnerabilidad
   confirmada + la pregunta clave: "¿Por qué existe?" (SQL concat, falta de
   sanitización de la salida) y "¿Cómo se arregla?" (queries parametrizadas,
   escapado de HTML al renderizar).
4. **Reporte DAST**: del `zap-report.html` extraer: total de alertas por riesgo
   (alto/medio/bajo) y top-3 incidencias con su URL y mitigación sugerida.
5. **Conclusiones**: qué casos pasan porque la app es "funcional", cuáles fallan
   y qué significa un fallo (bug de funcionalidad vs. hallazgo de seguridad).

## 5. Solución de problemas

| Síntoma | Causa probable | Acción |
|---------|----------------|--------|
| `Connection refused` a selenium | El node no terminó de subir | `docker compose ps`, espera; reintenta |
| `NoSuchElement` / timeouts intermitentes | SPA aún renderizando | Los steps ya incluyen esperas; si persiste, sube los `Duration` en `Ui.java` |
| `permission denied` en `evidencias/` | Docker sin permisos de la carpeta | `chmod -R 777 evidencias` (solo lab) |
| El hook ZAP no genera reporte | ZAP aún cargando cuando termina la suite | Reintenta la corrida; si persiste, baja `ZAP_ACTIVE_SCAN` |
| Juice Shop no levanta | Puerto 3000 ocupado | Cambia el mapeo en `docker-compose.yml` |
| Quiero correr un solo UC | Filtro por tag | `docker compose run --rm tests mvn test -Dcucumber.filter.tags=@uc09` |

## 6. Comandos rápidos de referencia

```bash
# levantar todo
docker compose up -d && docker compose ps

# correr la suite (re-ejecutable)
docker compose run --rm tests

# correr solo los tests de seguridad
docker compose run --rm tests mvn test -Dcucumber.filter.tags=@seguridad

# correr solo los funcionales, sin active scan (rápido)
docker compose run --rm -e ZAP_ACTIVE_SCAN=false tests mvn test -Dcucumber.filter.tags=@funcional

# abrir los reportes
open bdd-tests/target/cucumber-report/cucumber.html
open evidencias/zap-report.html

# listar evidencias
ls -lh evidencias/

# bajar todo
docker compose down -v
```
