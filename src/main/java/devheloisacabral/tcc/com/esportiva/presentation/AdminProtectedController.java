package devheloisacabral.tcc.com.esportiva.presentation;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AdminProtectedController {

    @PostMapping("/admin/protected")
    public Map<String, String> execute() { return Map.of("status", "executed"); }
}
