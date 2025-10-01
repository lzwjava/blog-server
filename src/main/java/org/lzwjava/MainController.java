package org.lzwjava;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    @CrossOrigin(origins = "*")
    @GetMapping("/bandwidth")
    public ResponseEntity<String> getBandwidth() {
        return ResponseEntity.ok("Hello World");
    }
}
