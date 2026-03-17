package org.example.Controller;

import org.example.Anotaciones.GetMapping;
import org.example.Anotaciones.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String index() {
        return "<h1>Greetings from Spring Boot!</h1><p>Try <a href='/hello'>/hello</a> or <a href='/pi'>/pi</a></p>";
    }

    @GetMapping("/pi")
    public String getPi() {
        return "<h1>Value of PI</h1><p>pi = " + Math.PI + "</p>";
    }

    @GetMapping("/hello")
    public String hello() {
        return "<h1>Hello World!</h1>";
    }
}