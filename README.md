# [Docker and Spring Boot](https://www.youtube.com/watch?v=hV2hcgztg-w&t=2476s)

Tutorial tomado del canal de [Get Arrays](https://www.youtube.com/watch?v=hV2hcgztg-w&t=2476s)

--- 

## Sobre el proyecto base

En este tutorial de **Docker and Spring Boot**, el tutor **Junior de Get Arrays** utiliza el proyecto de su curso
**Full Stack Spring Boot API with Angular (ADVANCED)**. Pero como no tengo acceso a ese curso, es que optaré por usar
el uno de sus proyectos de youtube: **Send Emails with Spring Boot** con algunas modificaciones que le realicé, como el
listar usuarios, obtener un usuario por su id. El objetivo, finalmente es ver cómo **dockerizar una aplicación de Spring
Boot** y cómo realizar las configuraciones correctas cuando se tienen credenciales o perfiles: dev, test, prod, etc.

> En resumen, utilizaré el proyecto de **envío de emails** para realizar este tutorial de **docker con spring boot**.

### Recordando el proyecto de envío de emails

Para saber los pasos seguidos en la construcción del proyecto de email ir a mi repositorio
[**spring-boot-email**](https://github.com/magadiflo/spring-boot-email).

Recordar que estamos trabajando con el perfil dev:

````yaml
spring:
  profiles:
    active: ${ACTIVE_PROFILE:dev}
````

Endpoint para listar usuarios:

````bash
curl -v http://localhost:8081/api/v1/users | jq
````

Endpoint para mostrar el usuario por su id:

````bash
curl -v http://localhost:8081/api/v1/users/1852 | jq
````

Endpoint para crear un usuario enviándole un correo:

````bash
curl -v -X POST -H "Content-Type: application/json" -d "{\"name\": \"Carlos Gonzales\", \"email\": \"carlos@gmail.com\", \"password\": \"12345\"}" http://localhost:8081/api/v1/users | jq
````

Endpoint para confirmar la cuenta:

````bash
 curl -v http://localhost:8081/api/v1/users/confirm?token=4c1d51ae-71ab-42fa-b734-82fee7850aa3 | jq
````
