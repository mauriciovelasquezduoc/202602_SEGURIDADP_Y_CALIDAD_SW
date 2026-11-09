# language: es
@uc01 @funcional
Característica: Visualizar el catálogo de productos
  Como cliente del sitio
  quiero ver el catálogo de productos disponibles
  para poder elegir qué comprar

  Escenario: La página principal muestra el catálogo completo
    Dado que el usuario accede al sitio Juice Shop
    Entonces el título de la página es "OWASP Juice Shop"
    Y el catálogo muestra al menos 10 productos con nombre y precio
