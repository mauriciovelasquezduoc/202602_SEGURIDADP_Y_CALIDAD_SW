# language: es
@uc04 @funcional
Característica: Iniciar sesión con credenciales válidas
  Como cliente registrado
  quiero iniciar sesión con mis credenciales
  para acceder a mi cuenta y realizar pedidos

  Escenario: El login válido autentica al usuario (admin semilla)
    Dado que el usuario está en la página de inicio de sesión
    Cuando inicia sesión con "admin@juice-sh.op" y "admin123"
    Entonces la sesión queda iniciada
    Y el menú de usuario muestra la cuenta
