# language: es
@uc09 @seguridad
Característica: Bypass de autenticación con SQL Injection
  Como atacante
  quiero inyectar una tautología SQL en el login
  para autenticarme sin credenciales legítimas

  Nota: este test valida que la VULNERABILIDAD existe en Juice Shop
  (app con fallos intencionales). Un test verde confirma el hallazgo.

  Escenario: La tautología ' or 1=1-- autentica a un atacante
    Dado que el atacante está en la página de inicio de sesión
    Cuando inicia sesión con la inyección SQL "' or 1=1--" y la contraseña "x"
    Entonces la autenticación se acepta sin credenciales válidas
