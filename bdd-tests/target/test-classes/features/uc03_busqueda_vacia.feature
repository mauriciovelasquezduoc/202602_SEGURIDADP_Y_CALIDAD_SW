# language: es
@uc03 @funcional
Característica: Búsqueda sin resultados
  Como cliente del sitio
  quiero recibir retroalimentación clara cuando no hay coincidencias
  para entender que mi búsqueda no arrojó productos

  Escenario: La búsqueda sin coincidencias no lista productos
    Dado que el usuario accede al sitio Juice Shop
    Cuando busca "zzqqxxyy"
    Entonces no se muestra ningún producto como resultado
