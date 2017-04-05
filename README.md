# Business Process Service
**ServiceID**: business-process-service

Business Process Service in the NIMBLE Microservice Infrastructure based on [Camunda](https://camunda.org/)
  
## Configuration

Base configuration can be found at src/main/resources/application.properties and bootstrap.yml.
[Spring Cloud Config](https://cloud.spring.io/spring-cloud-config/) is used for central configuration management. A central configuration is hosted on [https://github.com/nimble-platform/cloud-config](https://github.com/nimble-platform/cloud-config)
and injected during startup.

## Swagger

The Business Process API is designed using the [swagger.io editor](http://editor.swagger.io) (file: src/main/resources/api.yml) and the code generator for the Spring framework. 
The Maven plugin (swagger-codegen-maven-plugin) is used to generate defined interfaces automatically in each Maven build.
In addition the Business Process Service provides a proxy to the REST API from Camunda.

## How-to

### Service build and startup

 ```bash
 # standalone
 mvn clean spring-boot:run
 
 # in docker environment from core cloud infrastructure using 8085 as internal port
 mvn clean package docker:build -P docker
 docker run -p 8081:8085 nimbleplatform/business-process-service
 ```
 The according Dockerfile can be found at src/main/docker/Dockerfile.

 Also you can reach the camunda cockpit available at http://localhost:8081/app/cockpit/default/ (kermit/superSecret)
 
### Get Version Request
 ```bash
 # get
 curl http://localhost:8081/version
  ```
 ---
 
### Business Process Example Calls

In the following there are some examples for interacting with Camunda through the REST interface.

* GET Engines 
```bash
 curl http://localhost:8081/rest/engine/
```
* GET list of process definitions of engine "default" 
```bash
 curl http://localhost:8081/rest/engine/default/process-definition/
```
* GET process definition of "Sample" 
```bash
 curl http://localhost:8081/rest/engine/default/process-definition/key/Sample/
```
* START 'Sample' process (without parameters)
```bash
 curl -X POST -H 'Content-Type:application/json' -d '{"variables": {}, "businessKey" : ""}' http://localhost:8081/rest/engine/default/process-definition/key/Sample/start
```
* START 'Order' process (with parameters)
```bash
 curl -X POST -H 'Content-Type:application/json' -d '{"variables": {"buyer": {"value":"myBuyer","type":"String"}, "seller":{"value":"mySeller","type":"String"}, "order":{"value":"<Order><item>myProduct</item></Order>","type":"String"}}, "businessKey" : ""}' http://localhost:8081/rest/engine/default/process-definition/key/Order/start
```
* GET a list of tasks for a specific process instance
```bash
 curl http://localhost:8081/rest/engine/default/task?processInstanceId={processInstanceId}
```
 
* COMPLETE a task of the process instance
```bash
 curl -X POST -H 'Content-Type:application/json' -d '{"variables": {"orderResponse":{"value":"<OrderResponse><item>approved</item></OrderResponse>","type":"String"}}, "businessKey" : ""}' http://localhost:8081/rest/engine/default/task/{taskId}/complete 
```

 
The project leading to this application has received funding from the European Union’s Horizon 2020 research and innovation programme under grant agreement No 723810.
