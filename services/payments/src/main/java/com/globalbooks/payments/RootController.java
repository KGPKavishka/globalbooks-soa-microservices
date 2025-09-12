package com.globalbooks.payments;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.*;

@RestController
public class RootController {
    @GetMapping("/")
    public Map<String, Object> root() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("service", "payments-service");
        m.put("version", "1.0.0");
        m.put("endpoints", Arrays.asList(
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus"
        ));
        return m;
    }
}