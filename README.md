# Chat application

## Run application using Spring Boot plugin

Assuming Java 13 (or greater) is on the path 
```bash
./mvnw clean install && ./mvnw -f server/ spring-boot:run
```
Then go to http://localhost:8080/chat/

## Run application as Docker container

Assuming Java 13 (or greater) is on the path and Docker daemon is running

* Build Docker image:
  ```bash
  ./mvnw clean install && ./mvnw -f server/ jib:dockerBuild
  ```
* Create and run Docker container:
  ```bash
  docker run -p 8080:8080 localhost/chat:latest
  ```
Then go to http://localhost:8080/chat/
