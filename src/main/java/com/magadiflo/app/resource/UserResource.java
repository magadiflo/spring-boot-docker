package com.magadiflo.app.resource;

import com.magadiflo.app.domain.HttpResponse;
import com.magadiflo.app.domain.User;
import com.magadiflo.app.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v1/users")
public class UserResource {

    private final IUserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(this.userService.findAllUsers());
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return this.userService.findUser(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<HttpResponse> createUser(@RequestBody User user) {
        User userDB = this.userService.saveUser(user);
        URI uriUser = URI.create("");
        HttpResponse httpResponse = HttpResponse.builder()
                .timeStamp(LocalDateTime.now().toString())
                .data(Map.of("user", userDB))
                .message("Usuario creado")
                .statusCode(HttpStatus.CREATED.value())
                .status(HttpStatus.CREATED)
                .build();
        return ResponseEntity.created(uriUser).body(httpResponse);
    }

    @GetMapping(path = "/confirm")
    public ResponseEntity<HttpResponse> confirmUserAccount(@RequestParam String token) {
        Boolean isSuccess = this.userService.verifyToken(token);
        HttpResponse httpResponse = HttpResponse.builder()
                .timeStamp(LocalDateTime.now().toString())
                .data(Map.of("success", isSuccess))
                .message("Cuenta verificada")
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
        return ResponseEntity.ok(httpResponse);
    }

}
