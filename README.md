# Rex - a clusterable scheduler
**Rex** (rex meaning "king" in Latin, the one that imposes order) is a microservice which serves as HTTP scheduler. 

**Rex** requires a graph of dependent tasks that need to be scheduled and ran in particular order. 

Each task has a HTTP request definition to `start` and `cancel`. These definitions are later used by Rex to start and 
cancel tasks remotely. Rex augments these requests with a callback for the remote service to report 
successful/unsuccessful completion of remote tasks. Additionally, there is an option to define a notification request 
for a task which will make Rex publish notifications as the task transitions between states.


# Running rex

## Requirements
- Apache Maven 3.8.1+
- OpenJDK 11
- Infinispan Server 11.0.3.Final+ (tested)
- Podman 3.2.3+ or Docker (integration-tests)

## Setting up an Infinispan Server
1. With script
- Run a bash script in `scripts/run-ispn.sh`
- The script 
  - will download correct version of ISPN
  - add a simple user "user" with password 1234 
  - run the ISPN server
- On repeated use, the server is started always fresh (no cache configurations) 
2. Manually (out-of-date)
- `wget https://downloads.jboss.org/infinispan/9.4.16.Final/infinispan-server-11.0.3.Final.zip`
- `unzip infinispan-server-11.0.3.Final.zip`
- `infinispan-server-11.0.3.Final/bin/standalone.sh -c clustered.xml` 
      (to make it available on LAN/WAN, supply the host/ip-address within additional option -b="ip-address", 
      otherwise it defaults to "localhost")
- wait until started
- open new terminal
- `infinispan-server-11.0.3.Final/bin/ispn-cli.sh --file="path/to/scheduler/server-config.cli"`
- wait for the server to reload

## Testing
- To run integration-tests (currently the only tests) you have to have Infinispan server running locally:
  1. Automatic deployment of ISPN with `testcontainers`
     - `testcontainers` require `podman` or `docker` to automatically run Infinispan in container
     - `docker`: no setup required (you can find more info on
     [testcontainers](https://www.testcontainers.org/supported_docker_environment/))
     - `podman`: follow tutorial on [Wiki](https://github.com/project-ncl/rex/wiki/Podman-set-up)

  2. Manual deployment of ISPN through script 
     - `./run-ispn.sh` and comment out all of `@QuarkusTestResource(InfinispanResource.class)` in all test classes

- `mvn clean test` to run tests
- **[Quarkus continuous testing](https://quarkus.io/guides/continuous-testing) is also possibility with `cd code && mvn quarkus:test`**

## Compilation and starting
- `mvn clean install -DskipTests`
- `java [-options] -jar core/target/quarkus-app/quarkus-run.jar`
    * additional options
       * -Dquarkus.infinispan-client.server-list=<ispn-ip-address>:11222 (REQUIRED) 
         * Host/ip-address of InfinispanHot-Rod server.
         * ALTERNATIVE: `export ISPN_NODE=<ispn-ip-address>:11222`
       * -Dscheduler.baseUrl=<scheduler-url>: (REQUIRED)
         * URL address of thescheduler which is used for callbacks sent to remote entities.
         * ALTERNATIVE: `export BASE_URL=<scheduler-url>`
       * -Dquarkus.http.port=<port>:
         * Port where the application is deployed on. Default value is 80.
       * -Dquarkus.infinispan-client.auth-username=<USER>
         * username for authentication to ISPN server
         * ALTERNATIVE: `export ISPN_USER=<USER>`
       * -Dquarkus.infinispan-client.auth-password=<PASSWORD>
         * password for authentication to ISPN server
         * ALTERNATIVE: `export ISPN_PASSWORD=<USER>`
- `/q/swagger-ui` is an OpenAPI endpoint

## Native compilation with GraalVM/Mandrel
- WARNING: scheduler will compile, but it was not tested properly
- `mvn clean install -Pnative` 
  - the compilation will take couple of minutes
- To run the native app run `core/target/core-<version>-runner <options>`

## Container image build
- mvn clean install -Dquarkus.container-image.build=true -Dquarkus.container-image.push=true

## Consuming rex-api in a Quarkus 3 application
Rex is currently based on Quarkus 2 which uses Jakarta EE 8. For any
application built on top of Quarkus 3 and/or Jakarta EE 9+ and consuming the
rex-api library, the following changes are needed in the application's pom.xml:

```xml
<dependency>
  <groupId>org.jboss.pnc.rex</groupId>
  <artifactId>rex-api</artifactId>
  <version>LATEST</version>
  <classifier>jakarta</classifier>
  <exclusions>
    <exclusion>
      <groupId>org.jboss.pnc.rex</groupId>
      <artifactId>rex-dto</artifactId>
    </exclusion>
  </exclusions>
</dependency>
<dependency>
  <groupId>org.jboss.pnc.rex</groupId>
  <artifactId>rex-dto</artifactId>
  <version>LATEST</version>
  <classifier>jakarta</classifier>
  <exclusions>
    <exclusion>
      <groupId>org.jboss.pnc</groupId>
      <artifactId>pnc-api</artifactId>
    </exclusion>
  </exclusions>
</dependency>
<dependency>
  <groupId>org.jboss.pnc</groupId>
  <artifactId>pnc-api</artifactId>
  <version>LATEST</version>
  <classifier>jakarta</classifier>
</dependency>
```
The rex-api with classifier `jakarta` is produced by the eclipse-transformer
maven plugin to modify the rex-api jar to port to `jakarta` import statements.
However this transformation doesn't take care of transitive dependencies. Since rex-dto also uses `javax` import statements, we need to:
- explicitly exclude rex-dto when using rex-api
- explicitly specify that we want to use the rex-dto with classifier `jakarta` and explicity exclude pnc-api
- explicitly specify that we want to use pnc-api with classifier `jakarta`

# Authentication
Rex is authentication using OIDC. Three roles are used:
- `pnc-app-rex-user`: Regular Rex user permissions for some endpoints
- `pnc-app-rex-editor`: Admin permissions for all endpoints
- `pnc-users-admin`: Admin permissions for all endpoints
