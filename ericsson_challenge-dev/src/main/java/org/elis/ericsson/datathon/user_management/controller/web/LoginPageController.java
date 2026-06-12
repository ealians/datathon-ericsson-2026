package org.elis.ericsson.datathon.user_management.controller.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class LoginPageController {

    @GetMapping("/login")
    public ModelAndView loginPage() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Se l'utente è già autenticato, reindirizzalo a /profiles
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return new ModelAndView("redirect:/profiles");
        }

        // Altrimenti, mostra la pagina di login
        return new ModelAndView("login");
    }
}
