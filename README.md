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

# Proyecto en Spring Boot

---

## Configuración application.yml

En nuestro archivo de configuración principal **application.yml** debemos crear variables de entorno con el **${}**.
Estas variables de entorno deben reemplazar a los valores que tengamos en duro, como por ejemplo, la contraseña y el
usuario de la base de datos, el puerto, el host del email, etc. de esa manera evitamos tener esos valores **sensibles**
definidos directamente en el archivo y en su reemplazo usamos las variables de entorno para poder pasar los valores en
tiempo de ejecución, ya sea usando IntelliJ IDEA o un Command Line (cmd).

Hasta este punto, el proyecto base vino configurado con las variables de entorno en el **application.yml**, como el
``${SERVER_PORT}, ${ACTIVE_PROFILE:dev}, ${POSTGRES_SQL_HOST}``, etc. aunque la variable **SERVER_PORT** lo cambiaremos
por el nombre de ``CONTAINER_PORT`` para que haga referencia al puerto que usará nuestra aplicación dentro del
contenedor Docker, es decir, nuestra aplicación de Spring Boot se ejecutará dentro del contenedor Docker en el puerto
que le asignemos a la variable **CONTAINER_PORT**.

A continuación, se muestra el archivo completo:

````yml
server:
  port: ${CONTAINER_PORT:8080}

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

En la configuración anterior vemos dos variables de entorno: **CONTAINER_PORT y ACTIVE_PROFILE**. Estas variables de
entorno tienen definido un valor predeterminado, es decir, por ejemplo, la siguiente variable
**${CONTAINER_PORT:8080}**, significa que ``cada vez que la aplicación se inicie, Spring intentará encontrar la
variable de entorno CONTAINER_PORT y si no lo encuentra, tomará por defecto el valor 8080.``, lo mismo ocurre para la
variable **ACTIVE_PROFILE**, si no lo encuentra, tomará por defecto el valor **dev**:

````yml
server:
  port: ${CONTAINER_PORT:8080}

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
CONTAINER_PORT: 8091
ACTIVE_PROFILE: dev

## Email Config
EMAIL_HOST: smtp.gmail.com
EMAIL_PORT: 587
EMAIL_ID: magadiflo@gmail.com
EMAIL_PASSWORD: qdonjimehiaemcku
VERIFY_EMAIL_HOST: http://localhost:${CONTAINER_PORT}

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
CONTAINER_PORT: 8092
ACTIVE_PROFILE: test

## Email Config
EMAIL_HOST: smtp.gmail.com
EMAIL_PORT: 587
EMAIL_ID: magadiflo@gmail.com
EMAIL_PASSWORD: qdonjimehiaemcku
VERIFY_EMAIL_HOST: http://localhost:${CONTAINER_PORT}

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
CONTAINER_PORT: 8093
ACTIVE_PROFILE: prod

## Email Config
EMAIL_HOST: smtp.gmail.com
EMAIL_PORT: 587
EMAIL_ID: magadiflo@gmail.com
EMAIL_PASSWORD: qdonjimehiaemcku
VERIFY_EMAIL_HOST: http://localhost:${CONTAINER_PORT}

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
 mvn spring-boot:run -Dspring-boot.run.arguments=--CONTAINER_PORT=4000
````

En el comando anterior estamos asignando el valor de 4000 a la variable de entorno **CONTAINER_PORT**, de esta manera la
aplicación se ejecutará con el perfil dev y nuevo puerto 4000.

---

# Docker

---

## Dockerfile

En esta sección configuraremos el archivo **Dockerfile** quien nos permitirá construir la imagen de nuestra aplicación.
Para esto, trabajaremos con un archivo **Dockerfile multi-stage**, es decir, **dividiremos la construcción de la imagen
en varias etapas.**

Mostraré a continuación el contenido de nuestro **Dockerfile** final, posteriormente iré explicando a detalle lo que
hace cada instrucción:

````dockerfile
# PRIMERA ETAPA
FROM maven:3.9.3 AS build
WORKDIR /app
ARG HOST_PORT
COPY pom.xml /app
RUN mvn dependency:resolve
COPY . /app
RUN mvn clean
RUN mvn package -DskipTests -X

# SEGUNDA ETAPA
FROM openjdk:17-jdk-alpine
COPY --from=build /app/target/*.jar app.jar
EXPOSE ${HOST_PORT}
CMD ["java", "-jar", "app.jar"]
````

### PRIMERA ETAPA: Creando el archivo .jar

- ``FROM maven:3.9.3 AS build``, estamos diciendo que usaremos como imagen base, la imagen de maven, versión 3.9.3 y
  **le damos el alias de build.**
- ``WORKDIR /app``, crearemos un directorio de trabajo donde colocaremos nuestra aplicación y desde
  donde trabajaremos, no es obligatorio, pero teniendo un directorio de trabajo nos aseguramos de
  saber exactamente dónde está nuestra aplicación y dónde se está ejecutando para que cuando se acceda al
  contenedor sepamos exactamente dónde buscar.
- ``ARG HOST_PORT``, creamos una variable con el ARG (de argumento) llamada HOST_PORT, porque queremos controlar el
  puerto del contendor desde el exterior del contenedor en ejecución, lo que significa que podemos pasarlo como
  una especie de variable de entorno, variable que podemos pasar de manera dinámica. **Lo hacemos de esta manera
  porque necesitamos hacerlo coincidir con lo que se ejecuta en la computadora.**
- ``COPY pom.xml /app``, **que copie el archivo pom.xml en el directorio de trabajo /app.** Como el Dockerfile está en
  la misma raíz que el pom.xml, por esa razón colocamos directamente pom.xml.
- ``RUN mvn dependency:resolve``, con RUN ejecutamos el comando maven **mvn dependency:resolve**, se utiliza para
  **resolver y descargar las dependencias del proyecto definidas en el archivo pom.xml**, que es el archivo de
  configuración de Maven. Este comando se centra en resolver las dependencias y descargar los artefactos necesarios para
  el proyecto. Estas dependencias serán descargadas en este contenedor temporal (build).
- ``COPY . /app``, que copie todo lo que está en la raíz del dockerfile (directorios, subdirectorios, archivos, etc) en
  nuestro directorio de trabajo /app.
- ``RUN mvn clean``, limpiamos todo, con esto se eliminará la carpeta /target.
- ``RUN mvn package -DskipTests -X``, para generar el .jar omitiendo las pruebas unitarias. **La bandera -X habilita el
  modo de depuración (verbose) para maven**. Con esta bandera, maven **imprimirá información detallada sobre lo que está
  haciendo**, como la configuración, las dependencias que resuelve y los pasos que sigue durante la construcción.

### SEGUNDA ETAPA: Construyendo la imagen

- ``FROM openjdk:17-jdk-alpine``, usaremos la imagen base para java, el openjdk:17-jdk-alpine.
- ``COPY --from=build /app/target/*.jar app.jar``, copiamos desde el paso 1 que tiene el alias build, y **lo que
  copiaremos se encuentra en /app/target y dentro de este último directorio estará nuestro .jar** quien tendrá un nombre
  extraño, pero no importa el nombre que tenga, solo nos interesa que sea el archivo .jar, solo tendremos un archivo
  .jar, así que para evitar colocar todo su nombre simplemente colocamos ``*.jar``. Ahora, ese .jar que se está
  copiando **le podemos dar un nombre, en nuestro caso le dimos el nombre de app.jar**
- ``EXPOSE ${HOST_PORT}``, expondremos el host port, haciendo referencia a la variable HOST_PORT de la línea que
  contiene el ARG de la primera etapa. Esta variable la pasaremos dinámicamente como el puerto para exponer nuestro
  contenedor con el exterior.

**NOTA**

> Recordar, como mencionó Andrés Guzmán en su curso de microservicios, el **EXPOSE** únicamente es a modo de
> documentación, para decirle al mundo qué puertos están disponibles.

- ``CMD ["java", "-jar", "app.jar"]``, le decimos el comando que usaremos para ejecutar nuestra aplicación java.

**DIFERENCIA ENTRE RUN Y CMD**

1. **RUN:** el comando RUN **se utiliza durante la fase de construcción del contenedor** para ejecutar comandos en una
   nueva capa de la imagen. Estos comandos **se ejecutan en el momento de la construcción y se utilizan para instalar
   paquetes, configurar entornos, copiar archivos, etc.** Cada instrucción RUN crea una nueva capa en la imagen, lo que
   permite que las capas se almacenen en caché y se reutilicen si los comandos no han cambiado.


2. **CMD:** La instrucción CMD se utiliza para proporcionar un comando por defecto que se ejecutará cuando se inicie el
   contenedor. **Esta instrucción se define una sola vez en el Dockerfile, y solo tendrá efecto en el contenedor cuando
   se inicie.** Si se proporciona un comando al iniciar el contenedor, reemplazará el comando CMD definido en el
   Dockerfile. **El comando CMD se usa principalmente para especificar el proceso principal que se ejecutará dentro del
   contenedor.**

## Docker Compose

Con **docker-compose** crearemos los contenedores a partir de la imagen generada por el **Dockerfile**. Para eso
necesitamos crear un archivo llamado **docker-compose.yml** donde configuraremos todo lo relacionado a la creación de
los contenedores:

````yaml
services:
  spring-boot-docker:
    container_name: ${CONTAINER_NAME}
    build:
      context: .
      args:
        HOST_PORT: ${HOST_PORT}
    image: spring-boot-docker:${TAG}
    restart: unless-stopped
    env_file:
      - ${ENV_FILE}
    expose:
      - ${HOST_PORT}
    ports:
      - ${HOST_PORT}:${CONTAINER_PORT}
````

Antes de iniciar con la explicación de cada línea del archivo anterior, definiremos lo que son los **services** en
docker-compose:

> Los servicios permiten definir cómo se ejecutan, escalan y se comunican entre sí los distintos contenedores de tu
> aplicación.

- ``spring-boot-docker``, es el nombre que le daremos a nuestro servicio.
- ``container_name: ${CONTAINER_NAME}``, nombre que le daremos al contenedor que se creará. El valor de la variable
  CONTAINER_NAME será pasado usando la shell, es decir la CLI de docker compose.
- ``context: .``, indica el path donde está ubicado el archivo **Dockerfile**, en nuestro caso en la raíz **(.)**.
- ``args: HOST_PORT: ${HOST_PORT}``, definimos la variable de entorno **HOST_PORT** que le pasaremos al archivo
  **Dockerfile**. Ahora, esta variable de entorno está recibiendo un valor dinámico **${HOST_PORT}** que será definida
  al momento de construir la imagen.
- ``image: spring-boot-docker:${TAG}``, le damos un nombre a nuestra imagen y una versión. El valor de la variable TAG
  será pasado a través de la shell (usando la CLI de docker compose).
- ``restart: unless-stopped``, significa que el servicio se reiniciará automáticamente cuando Docker se inicie o si el
  servicio se detiene por cualquier motivo excepto si el usuario lo detiene manualmente.
- ``env_file: - ${ENV_FILE}``, se utiliza para cargar variables de entorno desde un archivo específico en el sistema de
  archivos del host. La variable ${ENV_FILE} debe ser reemplazada por el nombre del archivo de variables de entorno que
  deseas usar.

**NOTA**

> Podríamos dejar el **EXPOSE del ${HOST_PORT}** solo en el Dockerfile, pero para estar seguros es que también hacemos
> el expose del puerto en el archivo docker-compose.yml

## Default Env File (Archivo de entorno predeterminado)

En la raíz del proyecto creamos un archivo del tipo **.env** donde definiremos las variables de entorno y sus valores
usadas en los archivos **docker-compose.yml y Dockerfile**:

``.env``

````properties
HOST_PORT=8000
CONTAINER_PORT=8000
````

Cuando ejecutemos el **docker-compose.yml**, este accederá a nuestro **Dockerfile** para construir la imagen docker de
la aplicación. Luego, iniciará el contenedor y **de forma predeterminada buscará el archivo .env**

Ahora, **el archivo .env creado anteriormente** es muy diferente al archivo que require la siguiente configuración
definida en el **docker-compose.yml:**

````yml
services:
  spring-boot-docker:
    env_file:
      - ${ENV_FILE}
````

La configuración anterior corresponde a las variables de entorno que requiere el contenedor, estas variables de entorno
estarán definidas dentro de un archivo **.env** y serán usadas cuando lancemos el contenedor. Observemos que estamos
pasándole una variable de entorno **${ENV_FILE}**, significa que cuando lancemos la ejecución del docker file debemos
pasarle el archivo correspondiente ya sea con valores de: dev, test o prod.

Mientras que para la construcción de la imagen, **sí requerimos el primer archivo .env**, quien contiene las dos
variables de entorno: HOST_PORT y el CONTAINER_PORT.

## Archivos Env específicos del entorno

**Crearemos los archivos .env correspondientes a los distintos entornos** donde ejecutaremos nuestra aplicación. En este
punto **podemos darle un nombre cualquiera**, en nuestro caso le daremos los siguientes:

- **.env.dev**, para el ambiente de desarrollo.
- **.env.test**, para el ambiente de pruebas.
- **.env.prod**, para el ambiente de producción.

### Env para el entorno de desarrollo

Crearemos el archivo ``.env.dev`` en la raíz del proyecto. Copiaremos todas las variables que hemos venido trabajando en
el archivo **application-dev.yml** y lo pegaremos en este nuevo archivo **.env.dev**.

**NOTA**

> Cuando ejecutemos nuestro proyecto, elegiremos utilizar algún archivo .env de entorno, ya sea el **.env.dev, .env.test
> o el .env.prod, estos archivos TENDRÁN PRIORIDAD sobre los archivos application-dev.yml, application-test.yml o
> application-prod.yml**

Las variables que tendrá nuestro archivo **.env.dev** serán las siguientes:

````properties
# Profile
ACTIVE_PROFILE=dev
# Database
POSTGRES_SQL_USERNAME=postgres
POSTGRES_SQL_PASSWORD=magadiflo
POSTGRES_SQL_HOST=192.168.0.10
POSTGRES_SQL_PORT=5432
POSTGRES_SQL_DB=db_spring_boot_email
# Host and Container
HOST_PORT=8081
CONTAINER_PORT=8081
# Email Config
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_ID=magadiflo@gmail.com
EMAIL_PASSWORD=${EMAIL_PASSWORD}
VERIFY_EMAIL_HOST=http://localhost:${CONTAINER_PORT}
# FrontEnd
UI_APP_URL=http://localhost:4200
````

**NOTA 1:** La variable **HOST_PORT**, no está definida en el **application-dev.yml** pero aquí sí la definimos. Esta
variable hace referencia al puerto exterior del contenedor, es decir, si estamos en nuestra pc local utilizaremos ese
puerto para poder comunicarnos con el contenedor y el contenedor usará su puerto interno que es el puerto CONTAINER_PORT
para comunicarse con nuestra aplicación.

**NOTA 2:** La variable **POSTGRES_SQL_HOST** contiene la ip 192.168.0.10, que es la ip de mi pc local donde se
encuentra instalado la base de datos de Postgresql, es decir, cuando se ejecute nuestra aplicación dentro del
contenedor, para conectarse a la base de datos deberá apuntar a mi pc local. Solo como acotación, allí no irá el
localhost o el 127.0.0.1, ya que nuestra aplicación estará en un contenedor y si dejamos con uno de esos valores, a
donde apuntará esa dirección será al localhost del contenedor y lo que nosotros queremos es que apunte a nuestra pc,
no al contenedor mismo.

**IMPORTANTE**

> Junior menciona que estos archivos: .env.dev, .env.test y el .env.prod, **no deberían tener los valores
> hardcodeados**, sino más bien, estos valores se deberían pasar uno a uno al momento de ejecutar el contenedor, bueno
> con excepción de la variable **ACTIVE_PROFILE** cuyo valor sí puede estar en duro, ya que solo es para seleccionar el
> perfil **dev**. Otra opción sería que en la máquina host, definir las variables de entorno desde donde se tomarán los
> valores.
>
> Por tema de ejemplo, solo pasaré dinámicamente la variable **EMAIL_PASSWORD=${EMAIL_PASSWORD}**, pero en el mundo
> real deberíamos no colocar en duro los valores, sino crear **otra variable de entorno** con el que recibirán
> dinámicamente los valores. Cuando digo, "otra variable de entorno" me refiero a que, por ejemplo:<br>
> EMAIL_PASSWORD, es en sí una variable de entorno, y le asignamos otra variable de entorno que recibirá dinámicamente:
> ${EMAIL_PASSWORD}.

Lo mismo haremos para el entorno de pruebas, creamos el archivo **.env.test** y le cambiamos sus valores en función a
ese entorno:

````properties
# Profile
ACTIVE_PROFILE=test
# Database
POSTGRES_SQL_USERNAME=postgres
POSTGRES_SQL_PASSWORD=magadiflo
POSTGRES_SQL_HOST=192.168.0.10
POSTGRES_SQL_PORT=5432
POSTGRES_SQL_DB=db_test
# Host and Container
HOST_PORT=8082
CONTAINER_PORT=8082
# Email Config
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_ID=magadiflo@gmail.com
EMAIL_PASSWORD=${EMAIL_PASSWORD}
VERIFY_EMAIL_HOST=http://localhost:${CONTAINER_PORT}
# FrontEnd
UI_APP_URL=http://localhost:4200
````

Y por último para el entorno de producción, creamos el archivo **.env.prod**:

````properties
# Profile
ACTIVE_PROFILE=prod
# Database
POSTGRES_SQL_USERNAME=postgres
POSTGRES_SQL_PASSWORD=magadiflo
POSTGRES_SQL_HOST=192.168.0.10
POSTGRES_SQL_PORT=5432
POSTGRES_SQL_DB=db_production
# Host and Container
HOST_PORT=8083
CONTAINER_PORT=8083
# Email Config
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_ID=magadiflo@gmail.com
EMAIL_PASSWORD=${EMAIL_PASSWORD}
VERIFY_EMAIL_HOST=http://localhost:${CONTAINER_PORT}
# FrontEnd
UI_APP_URL=http://localhost:4200
````

## Script de Inicio

Hasta este punto ya podríamos ejecutar el **docker-compose.yml**, pero lo haremos de una forma más sencilla aún. Lo
que haremos será **envolver en un script de shell** los comandos Docker, de esa forma se nos hará más sencillo la
ejecución.

En la raíz del proyecto creamos tres archivos **script de shell** correspondientes a cada entorno de ejecución.
Los archivos pueden tener cualquier nombre, pero deben terminar con la extensión **.sh**:

``start-dev.sh``

````shell
CONTAINER_NAME=app-container-dev TAG=dev ENV_FILE=./.env.dev docker-compose --env-file ./.env.dev up -d --build
````

``start-test.sh``

````shell
CONTAINER_NAME=app-container-test TAG=test ENV_FILE=./.env.test docker-compose --env-file ./.env.test up -d --build
````

``start-prod.sh``

````shell
CONTAINER_NAME=app-container-prod TAG=prod ENV_FILE=./.env.prod docker-compose --env-file ./.env.prod up -d --build
````

**DONDE**

De los archivos script de shell anteriores, se describen a continuación las
[**variables de entorno**](https://docs.docker.com/compose/environment-variables/set-environment-variables/) que nuestro
archivo **docker-compose.yml** espera recibir:

- **CONTAINER_NAME**, le asignamos el nombre que tendrá nuestro contenedor cuando se levante.
- **TAG**, le asignamos la versión que tendrá nuestra imagen creada.
- **ENV_FILE**, le asignamos el archivo **.env.<entorno>** (dependiendo del entorno) que está ubicado justamente al
  mismo nivel del archivo **docker-compose.yml**. Es importante resaltar que las variables de entorno que tiene el
  archivo que se está asignando, **se usarán al momento de levantar el contenedor**, es decir, el contenedor que se cree
  usará las variables de entorno definidas en el archivo asignado a la variable ENV_FILE, esto es, porque esta variable
  está siendo usada en el atributo **env_file**, que precisamente se usa para definir las variables que usarán los
  contenedores a crear, tal como se ve a continuación:

  ````
  env_file:
    - ${ENV_FILE}
  ````

- **-d**, modo separado (detached): ejecutar contenedores en segundo plano.
- **--build**, cree imágenes antes de iniciar contenedores. Esta construcción se hará en función de la siguiente
  configuración:

  ````
  build:
    context: .
    args:
      HOST_PORT: ${HOST_PORT}
  ````

  Por defecto, como nuestro archivo **Dockerfile** está al mismo nivel en ruta que el **docker-compose.yml**, será usado
  precisamente para hacer la construcción de la imagen.

- ``docker-compose --env-file ./.env.dev up``, este último comando lo dejé al final porque merece una explicación
  detallada. Si tenemos muchas variables de entorno que están dentro de un archivo y queremos pasarlos al momento de
  ejecutar el ``docker-compose up``, lo podemos hacer colocando en medio de los dos comandos anteriores la
  bandera ``--env-file`` seguido de la ruta y el archivo que contiene dichas variables ``./.env.dev``.

**IMPORTANTE**
> Si el ``--env-file`` no se usa en la línea de comando, el archivo ``.env`` se cargará de forma predeterminada. Eso
> significa, según nuestro caso, que el archivo ``.env`` que contiene los datos que mostramos en la parte inferior
> son los que serán cargados.

````dotenv
HOST_PORT=8000
CONTAINER_PORT=8000
````

**NOTA**
> En nuestro caso usamos ``docker-compose --env-file ./.env.dev up`` porque queremos usar de este archivo de entorno las
> variables HOST_PORT y CONTAINER_PORT que serán usadas para construir la imagen y exponer los puertos de los
> contenedores.
>
> Pero si nos damos cuenta, en los script de shell ya estamos pasando el archivo .env.dev (test o prod) en la variable:
> ``ENV_FILE=./.env.dev``, pero ojo, si observamos el **docker-compose.yml** veremos que esa variable será usada para
> **levantar contenedores**, mientras que el **docker-compose.yml** requiere variables de entorno propios, para por
> ejemplo, definir los puertos **externo:interno**
>
> Finalmente, como se mencionó en el apartado de **IMPORTANTE** si nosotros omitimos esta configuración
> ``--env-file ./.env.dev`` (significa que se ejecutará: ``docker-compose up``), se tomará por defecto el archivo
> ``.env`` cuyos valores de HOST_PORT y CONTAINER_PORT no coincidirán con los definidos en las variables de entorno
> del archivo .env.dev, o .env.test o .env.prod que están siendo pasadas en ``ENV_FILE=./.env.dev`` para la creación
> del contenedor.

### ¿Cómo obtiene los valores para las variables HOST_PORT y CONTAINER_PORT?

**De manera predeterminada Docker buscará un archivo llamado .env** para utilizar sus variables definidas, en nuestro
caso sí tenemos creado dicho archivo con dos variables de entorno: HOST_PORT y CONTAINER_PORT, pero como explicamos
anteriormente al haber definido de esta manera: ``docker-compose --env-file ./.env.dev up``, estamos siendo explícitos
por lo que ya no tomará por defecto el archivo ``.env``, sino más bien el archivo que le estamos definiendo, en este
caos el archivo ``.env.dev`` y de este archivo tomará sus variables HOST_PORT y CONTAINER_PORT.

## Despliegue

**Usando el Git Bash de Git** ejecutaremos el **script de shell** para crear la imagen y levantar el contenedor. Es
importante usar esta línea de comandos, ya que los archivos **script de shell** están asociados con sistemas operativos
como Linux. Si ejecuto esta instrucción usando el cmd de windows no se podrá, porque no detectará el **.sh** ni las
variables de entorno que le pase por la línea de comando.

Entonces, usando el **Git Bash de Git** ejecutamos el siguiente comando:

````bash
EMAIL_PASSWORD=qdonjimehiaemcku ./start-dev.sh
````

Observamos que además de ejecutar nuestro **script de shell** (start-dev.sh), le estamos pasando previamente una
variable de entorno **EMAIL_PASSWORD**, esto es porque en ningún archivo de propiedad, ni archivo de entorno hemos
definido el valor de esta variable, pero sí estos archivos de propiedad o de entorno, esperan recibir un valor para
EMAIL_PASSWORD.

Nuestra intención no es guardar datos sensibles en los archivos, en este caso en los **script de shell**, sino más bien,
**el valor de EMAIL_PASSWORD lo pasaremos justo al momento de ejecutar el archivo sh**, aunque en realidad, deberíamos
haber creado las variables para todas las otras variables que están en el archivo .env.dev (y todos los archivos de
entorno) y pasarlos al momento de ejecutar el archivo sh tal como lo haremos con la variable EMAIL_PASSWORD, pero como
son muchas variables, solo estamos ejemplificando con el EMAIL_PASSWORD.

**IMPORTANTE**

> Como estamos usando Postgresql que está instalado en mi pc local y nuestra aplicación estará desplegada en un
> contenedor, para que se comunique desde dentro del contenedor hacia afuera, es decir, hacia mi pc local, quien por
> cierto tiene la ``ip 192.168.0.10`` debemos configurar postgresql para que admita solicitudes de esa ip, sino marcará
> el siguiente error:<br>
> ``org.postgresql.util.PSQLException: FATAL: no hay una l nea en pg_hba.conf para  192.168.0.10 , usuario  postgres ,
> base de datos  db_spring_boot_email , sin cifrad``.<br>
>
> Para solucionarlo abrimos el archivo ``C:\Program Files\PostgreSQL\14\data\pg_hba.conf``, agregamos la siguiente
> configuración y reiniciamos postgresql:<br>
>
> ``#TYPE  DATABASE        USER            ADDRESS                 METHOD``</br>
> ``host    all             all             192.168.0.10/32         trust``