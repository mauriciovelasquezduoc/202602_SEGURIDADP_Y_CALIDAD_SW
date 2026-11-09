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
