package codehumane.reactorplayground;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@SpringBootApplication
public class ReactorPlaygroundApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReactorPlaygroundApplication.class, args);
    }

}

@Component
class ReactorPlaygroundHandler {

    private final WebClient client;

    public ReactorPlaygroundHandler(WebClient.Builder builder) {
        client = builder.baseUrl("http://localhost:8080").build();
    }

    public Mono<ServerResponse> doOnNextAfterOnErrorResume(ServerRequest request) {

        return client
                .get()
                .uri("/receive")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Greeting.class)
                .onErrorResume(this::mapToResponseException)
                .doOnNext(this::validate)
                .flatMap(this::toResponse);
    }

    private void validate(Greeting greeting) {
        if (greeting.getMessage().isEmpty()) {
            throw new InvalidGreetingException("empty name");
        }
    }

    private Mono<Greeting> mapToResponseException(Throwable e) {
        if (e instanceof InvalidGreetingException) {
            return Mono.error(new BadRequestException(e));
        } else {
            return Mono.error(new InternalServerErrorException());
        }
    }

    private Mono<ServerResponse> toResponse(Greeting greeting) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromValue(greeting));
    }

}

@Component
class ReceiveHandler {

    public Mono<ServerResponse> receive(ServerRequest request) {
        return Mono
                .just(request.queryParam("name").orElse(""))
                .flatMap(this::toResponse);
    }

    private Mono<ServerResponse> toResponse(String greeting) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromValue(new Greeting(greeting)));
    }
}

@Configuration(proxyBeanMethods = false)
class GreetingRouter {

    @Bean
    public RouterFunction<ServerResponse> routePlayground(ReactorPlaygroundHandler handler) {
        return RouterFunctions
                .route(GET("/doOnNextAfterOnErrorResume").and(accept(MediaType.APPLICATION_JSON)), handler::doOnNextAfterOnErrorResume);
    }

    @Bean
    public RouterFunction<ServerResponse> routeReceive(ReceiveHandler handler) {
        return RouterFunctions
                .route(GET("/receive").and(accept(MediaType.APPLICATION_JSON)), handler::receive);
    }

}

class Greeting {

    private String message;

    public Greeting() {
    }

    public Greeting(String message) {
        this.message = message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

class InvalidGreetingException extends RuntimeException {
    public InvalidGreetingException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class InternalServerErrorException extends RuntimeException {

}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException extends RuntimeException {
    public BadRequestException(Throwable cause) {
        super(cause);
    }
}
