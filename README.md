# event-driven-microservice

This repository is based on Sergey Kargopolov's course Event-Driven Microservices, CQRS, SAGA, Axon, Spring Boot.  

The detailed description can be found here: (https://www.udemy.com/course/spring-boot-microservices-cqrs-saga-axon-framework/learn/lecture/26488184?start=30#overview).

# Architecture

We have Eureka running in `DiscoveryServer` project to allow server registration.

We have 4 backend services (Products, Orders, Users and Payments)
Each server gets registered in the `DiscoveryServer` (Eureka) at startup.

We have an `ApiGateway` to work as a facade for the external environment.
It works as a client for Eureka in order to get the actual endpoint of the backend.
Automatic mapping is setup (`product-service` --> `ProductService`)

AxonServer is there in order to run distributed transactions via CQRS pattern.

Shared project is a dependency for Product and Order in order to share definitions for Commands and Events

# Run it
1. Start Axon Server running "java -jar axonserver.jar" under the Axon Server directory.
2. Start the `DiscoveryServer` (Eureka)
2. Start the backend layer `ProductService`
3. Start the `ApiGateway`

Check Eureka dashboard to confirm the service registration:
Find "Instances currently registered with Eureka" at (http://localhost:8761/)
Now, you should be able to send any request to http://localhost:8082/{backend-identifier}/{backend-endpoint} with the right parameters (GET, POST,...) and it should work


# How it works

## flow
1. `OrderService` will create a `ReserveProductCommand`that will be handled by `ProductService` (look at how the Entity match)
2. `ProductService` will handle the command and 

## CreateProduct
1. Issue a POST request to the `ProductsCommandController` REST endpoint
2. It will post a `CreateProductEvent` to the event-bus, this event will be:
   3. Intercepted by CreateProductCommandInterceptor and if it already exists, it will not send it 
   4. Handled by 
      5. ProductAggregate constructor to build the domain object
      6. ProductLookupEventsHandler to save it for future (and skip it next time)
      7. ProductsEventHandler to save the new Product in the database
5. If any of the EventHandlers throws an Exception, it will rollback for all.
   6. The exception is catched in the handler `@ExceptionHandler(resultType=Exception.class)` and then rethrown 
   7. and catched by the `ProductsServiceEventsErrorHandler` and will be rethrown 
   8. and catched by `ProductsServiceErrorHandler` that will send a response to the RestClient.
9. If no Exceptions raised, the Product is saved in the database
 