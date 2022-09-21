package org.dtodo1paco.microservices.cqrs.ProductsService.command.interceptors;

import java.util.List;
import java.util.function.BiFunction;

import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.dtodo1paco.microservices.cqrs.ProductsService.core.data.ProductLookupEntity;
import org.dtodo1paco.microservices.cqrs.ProductsService.core.data.ProductLookupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.dtodo1paco.microservices.cqrs.ProductsService.command.CreateProductCommand;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CreateProductCommandInterceptor implements 
						MessageDispatchInterceptor<CommandMessage<?>> {
	
	private final ProductLookupRepository productLookupRepository;
	
	@Autowired
	public CreateProductCommandInterceptor(ProductLookupRepository productLookupRepository) {
		this.productLookupRepository = productLookupRepository;
	}



	@Override
	public BiFunction<Integer, CommandMessage<?>, CommandMessage<?>> handle(
			List<? extends CommandMessage<?>> messages) {
		
		return (index, command)->{
			
			log.info("Intercepted command: " + command.getPayloadType());
			
			if(CreateProductCommand.class.equals(command.getPayloadType())) {
				
				CreateProductCommand createProductCommand = (CreateProductCommand)command.getPayload();

				log.info("check if it is an already existing product: {}", createProductCommand);

				// Check if the product already exists:
				ProductLookupEntity productLookupEntity = productLookupRepository.findByProductIdOrTitle(
							createProductCommand.getProductId(), createProductCommand.getTitle());
					if(productLookupEntity != null) {
						throw new IllegalStateException(
										String.format("Product with productId %s or title %s already exists", 
										createProductCommand.getProductId(), createProductCommand.getTitle())
										);
						}
					
				}
			
			return command;
		};
	}

}
