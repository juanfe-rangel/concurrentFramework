
package org.example.Controller;

import java.util.concurrent.atomic.AtomicLong;

import org.example.Anotaciones.GetMapping;
import org.example.Anotaciones.RequestParam;
import org.example.Anotaciones.RestController;

@RestController
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/greeting")
    public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return "Hola " + name;
    }
}
