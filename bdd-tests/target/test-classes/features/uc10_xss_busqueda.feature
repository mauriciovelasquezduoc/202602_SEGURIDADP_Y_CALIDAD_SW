# language: es
@uc10 @seguridad
Característica: XSS reflejado en el buscador
  Como atacante
  quiero inyectar un iframe con javascript: en la búsqueda
  para ejecutar código en el navegador de otros usuarios

  Nota: este test valida que la VULNERABILIDAD existe en Juice Shop
  (app con fallos intencionales). Un test verde confirma el hallazgo.

  Escenario: El payload de XSS se ejecuta y despliega una alerta
    Dado que el atacante abre el buscador
    Cuando inyecta la carga útil
      """
      <iframe src="javascript:alert('xss')">
      """
    Entonces la aplicación ejecuta el script y abre una ventana de alerta
