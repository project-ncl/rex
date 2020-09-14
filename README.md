Clusterable Task Scheduler

# Running the scheduler

## Requirements
- Apache Maven 3.6.3+
- OpenJDK 11
- Infinispan Server 9.4.16.Final (currently)

## Setting up an Infinispan Server
- `wget https://downloads.jboss.org/infinispan/9.4.16.Final/infinispan-server-9.4.16.Final.zip`
- `unzip infinispan-server-9.4.16.Final.zip`
- `infinispan-server-9.4.16.Final/bin/standalone.sh -c clustered.xml` 
      (to make it available on LAN/WAN, supply the host/ip-address withan additional option -b="ip-address", 
      otherwise it defaults to "localhost")
- wait until started
- open new terminal
- `infinispan-server-9.4.16.Final/bin/ispn-cli.sh --file="path/to/scheduler/server-config.cli"`
- wait for the server to reload

## Compilation and starting
- `mvn clean install -DskipTests` (tests require Inf. Server on localhost)
- `java [-options] -jar core/target/core-<version>-runner.jar`
    * additional options
       * -Dquarkus.infinispan-client.server-list=<ispn-ip-address>:11222:
            Host/ip-address of InfinispanHot-Rod server. Default value is "localhost:11222"
       * -Dscheduler.baseUrl=<scheduler-url>: 
            URL address of thescheduler which is used for callbacks sent to remote enti-ties. Default value is "http://localhost:8080/"
       * -Dquarkus.http.port=<port>:
            Port where the applicationis deployed on. Default value is 8080.
