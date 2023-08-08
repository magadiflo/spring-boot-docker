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

---

# Docker and Spring Boot

---

## Configuración application.yml

En nuestro archivo de configuración principal **application.yml** debemos crear variables de entorno con el **${}**.
Estas variables de entorno deben reemplazar a los valores que tengamos en duro, como por ejemplo, la contraseña y el
usuario de la base de datos, el puerto, el host del email, etc. de esa manera evitamos tener esos valores **sensibles**
definidos directamente en el archivo y en su reemplazo usamos las variables de entorno para poder pasar los valores en
tiempo de ejecución, ya sea usando IntelliJ IDEA o un Command Line (cmd).

Hasta este punto, el proyecto base vino configurado con las variables de entorno en el **application.yml**, como el
``${SERVER_PORT}, ${ACTIVE_PROFILE:dev}, ${POSTGRES_SQL_HOST}``, etc. A continuación, se muestra el archivo completo:

````yml
server:
  port: ${SERVER_PORT:8080}

spring:
  profiles:
    active: ${ACTIVE_PROFILE:dev}

  datasource:
    url: jdbc:postgresql://${POSTGRES_SQL_HOST}:${POSTGRES_SQL_PORT}/${POSTGRES_SQL_DB}
    username: ${POSTGRES_SQL_USERNAME}
    password: ${POSTGRES_SQL_PASSWORD}

  jpa:
    generate-ddl: true
    show-sql: true
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        jdbc:
          time_zone: America/Lima
        globally_quoted_identifiers: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  mail:
    host: ${EMAIL_HOST}
    port: ${EMAIL_PORT}
    username: ${EMAIL_ID}
    password: ${EMAIL_PASSWORD}
    default-encoding: UTF-8
    properties:
      mail:
        mime:
          charset: UTF-8
        smtp:
          writetimeout: 10000
          connectiontimeout: 10000
          timeout: 10000
          auth: true
          starttls:
            enable: true
            required: true
    # Configuración propia personalizada
    verify:
      host: ${VERIFY_EMAIL_HOST}

# Configuración propia personalizada
ui:
  app:
    url: ${UI_APP_URL}
````

En la configuración anterior vemos dos variables de entorno: **SERVER_PORT y ACTIVE_PROFILE**. Estas variables de
entorno tienen definido un valor predeterminado, es decir, por ejemplo, la siguiente variable **${SERVER_PORT:8080}**,
significa que ``cada vez que la aplicación se inicie, Spring intentará encontrar la variable de entorno SERVER_PORT y si
no lo encuentra, tomará por defecto el valor 8080.``, lo mismo ocurre para la variable **ACTIVE_PROFILE**, si no lo
encuentra, tomará por defecto el valor **dev**:

````yml
server:
  port: ${SERVER_PORT:8080}

spring:
  profiles:
    active: ${ACTIVE_PROFILE:dev}
````

Además, se han definido **configuraciones personalizadas**, una de ellas es el ``ui:app:url:`` que contiene la
variable de entorno ``${UI_APP_URL}`` que apunta a la **url de nuestra aplicación front-end**. Esta configuración es
importante, ya que cuando un usuario se registra debemos enviarle un correo electrónico con un enlace para que pueda
hacer clic en él. Recordemos que este enlace apunta al front-end. La configuración en el **application.yml** es el
siguiente:

````yml
ui:
  app:
    url: ${UI_APP_URL}
````

Una vez haga clic en el enlace enviado al correo, lo llevará a la url de nuestra aplicación front-end, desde allí,
recién debería llamar al backend (al endpoint de confirmación). Ahora, es importante recordar que hasta este momento lo
que enviamos como url al correo del usuario que se registra, es la url del backend mismo, veamos:

````java

@RequiredArgsConstructor
@Service
public class EmailServiceImpl implements IEmailService {
    @Value("${spring.mail.verify.host}") //<-- Tomando la url del backend
    private String host;

    @Override
    @Async
    public void sendHtmlEmail(String name, String to, String token) {
        /* código simplificado */
        context.setVariables(Map.of(
                "name", name,
                "url", EmailUtils.getVerificationUrl(this.host, token),
                "currentdate", LocalDateTime.now())
        );

    }
}
````

````java
public class EmailUtils {
    public static String getVerificationUrl(String host, String token) {
        return String.format("%s/api/v1/users/confirm?token=%s", host, token);
    }
}
````

> Por lo tanto, solo para dejar anotado, la url que enviamos al correo del usuario registrado debería cambiar y apuntar
> a la url del front-end.

Otra configuración personalizada que se agregó fue la siguiente:

````yml
spring:
  mail:
    verify:
      host: ${VERIFY_EMAIL_HOST}
````

Según Junior, esta variable de entorno representa la url de nuestro servidor de email. Hasta este punto no se observa
el lugar de su uso, ya que el que se está usando hasta este punto ``@Value("${spring.mail.verify.host}") `` en el
**EmailServiceImpl** correspondería en realidad a la url del front-end ``ui.app.url``.

## Configuración específica del entorno: dev, test, prod

Ahora, definiremos las variables de entorno y sus valores en los archivos de configuración dependiendo del entorno en el
que serán ejecutados, veamos:

``application-dev.yml``

````yml
#Database
POSTGRES_SQL_USERNAME: postgres
POSTGRES_SQL_PASSWORD: magadiflo
POSTGRES_SQL_HOST: 127.0.0.1
POSTGRES_SQL_PORT: 5432
POSTGRES_SQL_DB: db_spring_boot_email

#Server
SERVER_PORT: 8081
ACTIVE_PROFILE: dev

## Email Config
EMAIL_HOST: smtp.gmail.com
EMAIL_PORT: 587
EMAIL_ID: magadiflo@gmail.com
EMAIL_PASSWORD: qdonjimehiaemcku
VERIFY_EMAIL_HOST: http://localhost:${SERVER_PORT}

UI_APP_URL: http://localhost:4200
````

``application-test.yml``

````yml
#Database
POSTGRES_SQL_USERNAME: postgres
POSTGRES_SQL_PASSWORD: magadiflo
POSTGRES_SQL_HOST: 127.0.0.1
POSTGRES_SQL_PORT: 5432
POSTGRES_SQL_DB: db_test

#Server
SERVER_PORT: 8082
ACTIVE_PROFILE: test

## Email Config
EMAIL_HOST: smtp.gmail.com
EMAIL_PORT: 587
EMAIL_ID: magadiflo@gmail.com
EMAIL_PASSWORD: qdonjimehiaemcku
VERIFY_EMAIL_HOST: http://localhost:${SERVER_PORT}

UI_APP_URL: http://localhost:4200
````

``application-prod.yml``

````yml
#Database
POSTGRES_SQL_USERNAME: postgres
POSTGRES_SQL_PASSWORD: magadiflo
POSTGRES_SQL_HOST: 127.0.0.1
POSTGRES_SQL_PORT: 5432
POSTGRES_SQL_DB: db_production

#Server
SERVER_PORT: 8083
ACTIVE_PROFILE: prod

## Email Config
EMAIL_HOST: smtp.gmail.com
EMAIL_PORT: 587
EMAIL_ID: magadiflo@gmail.com
EMAIL_PASSWORD: qdonjimehiaemcku
VERIFY_EMAIL_HOST: http://localhost:${SERVER_PORT}

UI_APP_URL: http://localhost:4200
````

Listo, si ejecutamos la aplicación funcionará correctamente eligiendo el ``application-dev.yml`` como ambiente de
configuración predeterminado, esto es gracias a que en el **application.yml** definimos la variable de entorno
**ACTIVE_PROFILE** y si Spring no encuentra esa variable definida, usará el predeterminado **dev**.

Ahora, si quisiéramos cambiar el valor de alguna variable de entorno al momento de ejecutar la aplicación, es decir,
no cambiando manualmente los valores en los archivos, sino ya sea utilizando **IntelliJ IDEA** o a través de la misma
línea de comandos, sí se podría realizar. Veamos cómo sería si quisiéra cambiar el puerto ejecutando la aplicación a
través de la línea de comandos:

Nos posicionamos en la raíz de nuestro proyecto de Spring Boot y ejecutamos el siguiente comando:

````bash
 mvn spring-boot:run -Dspring-boot.run.arguments=--SERVER_PORT=4000
````

En el comando anterior estamos asignando el valor de 4000 a la variable de entorno **SERVER_PORT**, de esta manera la
aplicación se ejecutará con el perfil dev y nuevo puerto 4000.

