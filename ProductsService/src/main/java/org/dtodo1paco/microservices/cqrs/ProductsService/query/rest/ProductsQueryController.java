package org.dtodo1paco.microservices.cqrs.ProductsService.query.rest;

import java.util.List;

import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.dtodo1paco.microservices.cqrs.ProductsService.query.FindProductsQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductsQueryController {

	
	@Autowired
	QueryGateway queryGateway;
	
	@GetMapping
	public List<ProductRestModel>getProducts(){
		
		FindProductsQuery findProductsQuery = new FindProductsQuery();
		List<ProductRestModel>products = queryGateway.query(
				findProductsQuery, ResponseTypes.multipleInstancesOf(ProductRestModel.class)).join();
		
		return products;
		
		
	}
}
