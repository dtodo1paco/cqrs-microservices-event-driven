package org.dtodo1paco.microservices.cqrs.ProductsService.query;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ResetHandler;
import org.axonframework.messaging.interceptors.ExceptionHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.dtodo1paco.microservices.cqrs.ProductsService.core.data.ProductEntity;
import org.dtodo1paco.microservices.cqrs.ProductsService.core.data.ProductsRepository;
import org.dtodo1paco.microservices.cqrs.ProductsService.core.events.ProductCreatedEvent;
import org.dtodo1paco.microservices.cqrs.shared.events.ProductReservationCancelledEvent;
import org.dtodo1paco.microservices.cqrs.shared.events.ProductReservedEvent;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
@Component
@ProcessingGroup("product-group")
public class ProductEventsHandler {
	
	private ProductsRepository productsRepository;
	
	@Autowired
	public ProductEventsHandler(ProductsRepository productsRepository) {
		this.productsRepository = productsRepository;
	}
	
	//General Exception handling
	@ExceptionHandler(resultType=Exception.class)
	public void handle(Exception exception) throws Exception {
		log.error("Handled Exception {}", exception);
		throw exception;
	}

	@ExceptionHandler(resultType=IllegalArgumentException.class)
	public void handle(IllegalArgumentException exception) {
		log.error("Handled IllegalArgumentException {}", exception);
		throw exception;
	}

	@EventHandler
	public void on(ProductCreatedEvent event) throws Exception {
		log.info("ProductEventsHandler: Handling ProductCreatedEvent: {}",event);

		ProductEntity productEntity = new ProductEntity();
		BeanUtils.copyProperties(event, productEntity);
		
		try {
			productsRepository.save(productEntity);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		Random rand = new Random();

		// Generate random integers in range 0 to 999
		int rand_int1 = rand.nextInt(10);
		if(rand_int1 < 1) { 		//Transaction will be rolled back: ProductLookupEntity will not be persisted
			throw new Exception("Forcing exception in the Event Handler class to be catched by the ListenerInvocationErrorHandler");
		}
		
	}
	
	@EventHandler
	public void on(ProductReservedEvent productReservedEvent) {
		
		ProductEntity productEntity = 
				productsRepository.findByProductId(productReservedEvent.getProductId());
		
		log.debug("ProductReservedEvent: Current product quantity: " + productEntity.getQuantity());
		
		productEntity.setQuantity(productEntity.getQuantity()-productReservedEvent.getQuantity());
		
		productsRepository.save(productEntity);
		
		log.debug("ProductReservedEvent: New product quantity: " + productEntity.getQuantity());
		
		log.info("productReservedEvent is called for productId: " + productReservedEvent.getProductId() + 
				" and orderId: " + productReservedEvent.getOrderId());
	}
	
	@EventHandler
	public void on(ProductReservationCancelledEvent  productReservationCancelledEvent) {
		
		ProductEntity currentlyStoredProduct = 
				productsRepository.findByProductId(productReservationCancelledEvent.getProductId());
		
		log.debug("ProductReservationCancelledEvent: Current product quantity: " +
		currentlyStoredProduct.getQuantity());
		
		int newQuantity =
				productReservationCancelledEvent.getQuantity() + currentlyStoredProduct.getQuantity();
		
		currentlyStoredProduct.setQuantity(newQuantity);
		
		productsRepository.save(currentlyStoredProduct);
		
		log.debug("ProductReservationCancelledEvent: New product quantity: " +
				currentlyStoredProduct.getQuantity());
		
	}
	
	@ResetHandler
	public void reset() {
		
		productsRepository.deleteAll();
	}
}
