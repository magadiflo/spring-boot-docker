# Corrigiendo el EXPOSE definido en el Dockerfile

---

**NOTA**
> Esta corrección es solo a modo de documentación, ya que si vamos al `Dockerfile` de este proyecto veremos que aún
> mantiene `EXPOSE ${HOST_PORT}` lo cual está mal, debería estar en `EXPOSE ${CONTAINER_PORT}`, pero realizar ese
> cambio implica cambiar en varios lugares de esta aplicación, así que lo dejaré así, pero ya sabemos que en realidad
> debería estar `EXPOSE ${CONTAINER_PORT}`.

En el `Dockerfile` de este proyecto y en la documentación `README.md` **he colocado `EXPOSE ${HOST_PORT}`, lo cual es
incorrecto.** Lo correcto sería definir `EXPOSE ${CONTAINER_PORT}`.

En Docker, el comando `EXPOSE` se utiliza para indicar qué puertos serán expuestos por el contenedor cuando se ejecute.
Sin embargo, **es importante destacar que `EXPOSE` no publica realmente los puertos. Lo que hace es documentar los
puertos en los que una aplicación dentro del contenedor escuchará conexiones.**

Por ejemplo, si tu `aplicación Spring Boot escucha en el puerto 8080` (el **puerto predeterminado para aplicaciones
Spring Boot**), tu archivo `Dockerfile` podría tener una línea como esta:

````dockerfile
EXPOSE 8080
````

Esto significa que la aplicación dentro del contenedor estará escuchando en el puerto 8080 para las conexiones
entrantes. Indica que el contenedor expone el puerto 8080 para ser accesible desde otros contenedores en la misma red
de Docker. Pero para acceder a ese puerto desde fuera del contenedor, deberías mapearlo al puerto de tu host cuando
ejecutes el contenedor:

````bash
$ docker run -p <HOST_PORT>:<CONTAINER_PORT> <nombre_de_la_imagen>
````

Donde `HOST_PORT` es el puerto de tu máquina local y `CONTAINER_PORT` es el puerto expuesto por tu aplicación Spring
Boot dentro del contenedor.

En resumen, en el comando `EXPOSE` de tu Dockerfile deberías usar el puerto interno de tu contenedor (usualmente el
puerto en el que tu aplicación Spring Boot está escuchando), que en tu caso podría ser la variable de entorno
`CONTAINER_PORT`. Luego, al ejecutar el contenedor, especificas cómo deseas que los puertos del host se relacionen con
los puertos expuestos del contenedor.

````dockerfile
EXPOSE ${CONTAINER_PORT}
````

# Libro: Aprender Docker

## Opción -P, --publish-all

Recordemos que cuando creamos un `Dockerfile` podemos definir una instrucción llamada `EXPOSE`, que según la
documentación oficial de Docker, es usado simplemente como documentación, pero si creamos un contenedor con la
opción `-P` (mayúscula) podemos hacer uso de ella.

Con la opción `-P` (en mayúscula) publicamos todos los puertos que se hayan expuesto en la imagen que se ha utilizado
para crear el contenedor, en puertos aleatorios del `host`.

En este caso, no tenemos que indicar el puerto local, sino que **será seleccionado aleatoriamente** entre los puertos
que estén libres. El puerto del contenedor con el que se hace la redirección estará definido en el arcivo `Dockerfile`
con el que se ha creado la imagen del contenedor **(usando la instrucción EXPOSE)**. Tenga en cuenta que, en el archivo
`Dockerfile`, se pueden exponer varios puertos a la vez.

Veamos el siguiente ejemplo:

En mi caso, mi aplicación de Spring Boot está usando el puerto `8085`:

````yaml
server:
  port: 8085

spring:
  application:
    name: products
````

Eso significa que el puerto que usará internamente el contenedor debe ser el `8085`.

Una vez construido el `.jar` generamos la imagen de la aplicación utilizando el siguiente `Dockerfile`:

````dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY ./target/*.jar ./app.jar
ARG CONTAINER_PORT=8085
EXPOSE ${CONTAINER_PORT}
CMD ["java", "-jar", "app.jar"]
````

Este `Dockerfile` expone el `CONTAINER_PORT` que es el puerto que tendrá el contenedor y donde estará siendo ejecutado
nuestra aplicación de Spring Boot, que es el puerto `8085`.

Ahora, construimos la imagen a partir del `Dockerfile` anterior:

````bash
$ docker container run -d -P --name c_products products
````

Es importante notar que estamos utilizando la opción `-P` (en mayúscula), esta opción nos permitirá **seleccionar un
puerto aleatorio** del `host` de Docker y se **redirigirá al puerto `8085` del contenedor.**

Listamos los contenedores para ver el que acabamos de crear:

````bash
$ docker container ls -a
CONTAINER ID   IMAGE                  COMMAND                  CREATED         STATUS             PORTS                              NAMES
b787f2484e07   products               "/__cacert_entrypoin…"   6 seconds ago   Up 4 seconds       0.0.0.0:32775->8085/tcp            c_products
````

Comprobamos que **el puerto aleatorio que se ha seleccionado para nuestro host Docker (pc local)** es el puerto `32775`
que está siendo enlazado al puerto `8085` del contenedor.

Comprobamos que podemos acceder al contenido del contenedor usando la terminal con el comando curl:

````bash
$ curl -v http://localhost:32775/api/v1/products/1 | jq
>
< HTTP/1.1 200 OK
< Content-Type: application/json
< Content-Length: 79
<
{
  "id": 1,
  "name": "Teclado Logitech",
  "price": 75.8,
  "image": "keyboard-logitech.png"
}
````