package org.dtodo1paco.microservices.cqrs.ProductsService.command;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ResetHandler;
import org.dtodo1paco.microservices.cqrs.ProductsService.core.data.ProductLookupEntity;
import org.dtodo1paco.microservices.cqrs.ProductsService.core.data.ProductLookupRepository;
import org.dtodo1paco.microservices.cqrs.ProductsService.core.events.ProductCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ProcessingGroup("product-group")
public class ProductLookupEventsHandler {
	
	private final ProductLookupRepository productLookupRepository;
	
	@Autowired
	public ProductLookupEventsHandler(ProductLookupRepository productLookupRepository) {
		this.productLookupRepository = productLookupRepository;
	}

	@EventHandler
	public void on(ProductCreatedEvent event) {
		log.info("ProductLookupEventsHandler: Handling ProductCreatedEvent: {}",event);
		ProductLookupEntity productLookupEntity = new ProductLookupEntity(
					event.getProductId(), event.getTitle());
		
		productLookupRepository.save(productLookupEntity);
	}
	
	@ResetHandler
	public void reset() {
		
		productLookupRepository.deleteAll();
	}

}
