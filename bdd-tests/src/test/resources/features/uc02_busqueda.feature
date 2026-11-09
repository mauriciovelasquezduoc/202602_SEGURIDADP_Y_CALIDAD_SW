# language: es
@uc02 @funcional
Característica: Buscar productos por nombre
  Como cliente del sitio
  quiero buscar productos por un término
  para encontrar rápidamente lo que necesito

  Escenario: La búsqueda con coincidencias lista productos relevantes
    Dado que el usuario accede al sitio Juice Shop
    Cuando busca "apple"
    Entonces se muestran al menos 2 resultados
    Y los resultados contienen "apple" en el nombre
