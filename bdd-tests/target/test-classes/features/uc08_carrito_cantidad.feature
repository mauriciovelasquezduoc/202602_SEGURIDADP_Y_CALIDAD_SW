# language: es
@uc08 @funcional
Característica: Gestionar cantidades del carrito
  Como cliente del sitio
  quiero cambiar la cantidad de un producto
  para comprar varios ejemplares y ver el total correcto

  Escenario: Cambiar la cantidad recalcula el subtotal
    Dado que el usuario accede al sitio Juice Shop
    Cuando añade "Apple Juice (1000ml)" al carrito
    Y abre el carrito
    Y cambia la cantidad de la primera fila a 2
    Entonces el subtotal corresponde al precio por la cantidad
