package xyz.crearts.note.keeper.config;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class FaviconController {

    @GetMapping("favicon.ico")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void favicon() {
    }
}
