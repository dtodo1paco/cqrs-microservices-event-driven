package org.dtodo1paco.microservices.cqrs.ProductsService.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/productsDeprecated")
@Deprecated(since = "splitted in command.rest and query.rest packages controller")
public class ProductsController {

  @Autowired
  private Environment env;

  @GetMapping
  public String getProduct() {
    return "HTTP GET handled " + env.getProperty("local.server.port");
  }

  @PutMapping
  public String updateProduct() {
    return "HTTP PUT handled " + env.getProperty("local.server.port");
  }

  @PostMapping
  public String createProduct() {
    return "HTTP POST handled " + env.getProperty("local.server.port");
  }

  @DeleteMapping
  public String deleteProduct() {
    return "HTTP DELETE handled " + env.getProperty("local.server.port");
  }
}
