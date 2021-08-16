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
- To run integration-tests (currently the only tests) you have to have Infinispan server running locally (`scripts/run-ispn.sh` is all that is needed)
  - (for the future `testcontainers` are considered)
- `mvn clean test` to run tests

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