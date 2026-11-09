# language: es
@uc06 @funcional
Característica: Registrar un nuevo usuario
  Como visitante del sitio
  quiero crear una cuenta
  para empezar a comprar

  Escenario: El registro con datos válidos se completa
    Dado que el usuario accede a la página de registro
    Cuando registra un nuevo usuario con respuesta de seguridad "Lab S14"
    Entonces el registro se completa con un mensaje de éxito
