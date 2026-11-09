# language: es
@uc05 @funcional
Característica: Rechazar credenciales inválidas
  Como cliente del sitio
  quiero que el sistema rechace logins con datos incorrectos
  para que nadie acceda a cuentas ajenas

  Esquema del escenario: Intentos de login inválidos
    Dado que el usuario está en la página de inicio de sesión
    Cuando inicia sesión con "<email>" y "<password>"
    Entonces la sesión no queda iniciada
    Y se muestra un mensaje de error

    Ejemplos:
      | email               | password      |
      | demo@juice-sh.op    | PasswordErrada|
      | inexistente@x.cl    | demo          |
