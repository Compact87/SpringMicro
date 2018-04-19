package com.example.demo;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
@EnableDiscoveryClient 
@SpringBootApplication
public class SpringCustomerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringCustomerApplication.class, args);
	}

	@Bean
	CommandLineRunner init(CustomerRespository customerRepository) {
		return (evt) -> {
			customerRepository.save(new Customer("Adam", "adam@boot.com"));
			customerRepository.save(new Customer("John", "john@boot.com"));
			customerRepository.save(new Customer("Smith", "smith@boot.com"));
			customerRepository.save(new Customer("Edgar", "edgar@boot.com"));
			customerRepository.save(new Customer("Martin", "martin@boot.com"));
			customerRepository.save(new Customer("Tom", "tom@boot.com"));
			customerRepository.save(new Customer("Sean", "sean@boot.com"));
		};
	}
}

@RefreshScope
@RestController
class CustomerController {
	@Value("${banned.customers}")
	private String bannedCustomers;

	CustomerRegistrar customerRegistrar;

	@Autowired
	CustomerController(CustomerRegistrar customerRegistrar) {
		this.customerRegistrar = customerRegistrar;
	}
	

	@RequestMapping(path = "/register", method = RequestMethod.POST)
	Customer register(@RequestBody Customer customer) {
		if (Arrays.asList(bannedCustomers.split(",")).contains(customer.getName())) {
			System.out.println("The customer is on the watchlist");
			return new Customer();
			
		}else	return customerRegistrar.register(customer);
		}
	}

	@Component
	class CustomerRegistrar {

		CustomerRespository customerRespository;
		Sender sender;

		@Autowired
		CustomerRegistrar(CustomerRespository customerRespository, Sender sender) {
			this.customerRespository = customerRespository;
			this.sender = sender;
		}

		Customer register(Customer customer) {
			Optional<Customer> existingCustomer = customerRespository.findByName(customer.getName());
			if (existingCustomer.isPresent()) {
				throw new RuntimeException("is already exists");
			} else {
				customerRespository.save(customer);
				sender.send(customer.getEmail());
				System.out.println("!--------------sending message at" + customer.getEmail() + "--------------!");
			}
			return customer;
		}
	}

	@Component
	class Sender {

		RabbitMessagingTemplate template;

		@Autowired
		Sender(RabbitMessagingTemplate template) {
			this.template = template;
		}

		@Bean
		Queue queue() {
			return new Queue("CustomerQ", false);
		}

		public void send(String message) {
			template.convertAndSend("CustomerQ", message);
		}
	}

	@RepositoryRestResource
	interface CustomerRespository extends JpaRepository<Customer, Long> {
		Optional<Customer> findByName(@Param("name") String name);
	}

@Entity
class Customer {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String name;
	private String email;
	
	public Customer (){}

	
	public Customer(String name, String email) {
		super();
		this.name = name;
		this.email = email;
	}


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public String toString() {
		return "Customer [id=" + id + ", name=" + name + ", email=" + email + "]";
	}
	
	 
	
}