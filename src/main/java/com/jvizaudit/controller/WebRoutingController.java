package com.jvizaudit.controller;
import com.jvizaudit.entity.CodeHistory;
import com.jvizaudit.entity.User;
import com.jvizaudit.repository.CodeHistoryRepository;
import com.jvizaudit.repository.UserRepository;
import com.jvizaudit.service.CodeExecutionService;
import com.jvizaudit.service.CodeFormatterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;
import com.jvizaudit.dto.UserDto;
import org.springframework.security.crypto.password.PasswordEncoder;

@Controller
public class WebRoutingController {
    @Autowired private UserRepository userRepo;
    @Autowired private CodeHistoryRepository historyRepo;
    @Autowired private CodeFormatterService formatter;
    @Autowired private CodeExecutionService executor;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping("/") public String home() { return "home"; }
    @GetMapping("/login") public String login() { return "login"; }
    
    @GetMapping("/editor")
    public String editor(@RequestParam(required=false) Integer id, Model model, Principal principal) {
        if(id != null && principal != null) {
            User u = userRepo.findByEmail(principal.getName()).orElse(null);
            if(u != null) {
                historyRepo.findByHistoryIdAndUser_UserId(id, u.getUserId())
                    .ifPresent(h -> model.addAttribute("code", h.getSourceCode()));
            }
        }
        return "editor";
    }

    @GetMapping("/history")
    public String history(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        User u = userRepo.findByEmail(principal.getName()).orElse(null);
        if (u == null) {
            return "redirect:/login";
        }
        model.addAttribute("histories", historyRepo.findByUser_UserIdOrderByUpdatedAtDesc(u.getUserId()));
        return "history";
    }

    @PostMapping("/api/format")
    @ResponseBody
    public String format(@RequestBody String code) {
        return formatter.format(code);
    }

    @PostMapping("/api/execute")
    @ResponseBody
    public String execute(@RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        String input = payload.getOrDefault("input", "");
        return executor.runCode(code, input);
    }

    @GetMapping("/api/user")
    @ResponseBody
    public UserDto getCurrentUser(Principal principal) {
        User u = userRepo.findByEmail(principal.getName()).orElse(null);
        if (u == null) return null;
        UserDto dto = new UserDto();
        dto.setEmail(u.getEmail());
        dto.setUsername(u.getUsername());
        dto.setRole(u.getRole());
        return dto;
    }

    @PostMapping("/api/history")
    @ResponseBody
    public String saveHistory(@RequestBody Map<String, String> payload, Principal principal) {
        try {
            User u = userRepo.findByEmail(principal.getName()).orElse(null);
            if(u == null) return "Unauthorized";
            
            CodeHistory history = new CodeHistory();
            history.setUser(u);
            history.setSourceCode(payload.get("sourceCode"));
            history.setHistoryName(payload.getOrDefault("historyName", "Untitled"));
            history.setStatus("Active");
            historyRepo.save(history);
            return "Saved";
        } catch(Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @DeleteMapping("/api/history/{id}")
    @ResponseBody
    public String deleteHistory(@PathVariable Integer id, Principal principal) {
        try {
            User u = userRepo.findByEmail(principal.getName()).orElse(null);
            if(u == null) return "Unauthorized";
            
            CodeHistory history = historyRepo.findByHistoryIdAndUser_UserId(id, u.getUserId()).orElse(null);
            if(history == null) return "NotFound";
            
            historyRepo.delete(history);
            return "Deleted";
        } catch(Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @PutMapping("/api/history/{id}")
    @ResponseBody
    public String updateHistory(@PathVariable Integer id, @RequestBody Map<String, String> payload, Principal principal) {
        try {
            User u = userRepo.findByEmail(principal.getName()).orElse(null);
            if(u == null) return "Unauthorized";
            
            CodeHistory history = historyRepo.findByHistoryIdAndUser_UserId(id, u.getUserId()).orElse(null);
            if(history == null) return "NotFound";
            
            String newName = payload.get("historyName");
            if(newName != null && !newName.trim().isEmpty()) {
                history.setHistoryName(newName.trim());
                historyRepo.save(history);
                return "Updated";
            }
            return "NoChanges";
        } catch(Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/registration")
    public String registrationForm() { 
        return "registration"; 
    }

    @PostMapping("/registration")
    public String registerUser(@ModelAttribute UserDto userDto, Model model) {
        if (userRepo.findByEmail(userDto.getEmail()).isPresent()) {
            model.addAttribute("error", "There is already an account registered with that email.");
            return "registration";
        }

        User newUser = new User();
        newUser.setUsername(userDto.getUsername());
        newUser.setEmail(userDto.getEmail());
        newUser.setRole(userDto.getRole().toLowerCase()); // 'student' or 'instructor'
        newUser.setPasswordHash(passwordEncoder.encode(userDto.getPassword()));

        userRepo.save(newUser);

        return "redirect:/login?success";
    }
    @GetMapping("/profile")
    public String profilePage(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User u = userRepo.findByEmail(principal.getName()).orElse(null);
        if (u == null) return "redirect:/login";
        
        model.addAttribute("user", u);
        return "profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam String oldPassword, @RequestParam String newPassword, 
                                 Principal principal, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        User u = userRepo.findByEmail(principal.getName()).orElse(null);
        
        if (u != null && passwordEncoder.matches(oldPassword, u.getPasswordHash())) {
            u.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepo.save(u);
            redirectAttributes.addFlashAttribute("success", "Password updated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Incorrect old password. Please try again.");
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/delete")
    public String deleteAccount(Principal principal, jakarta.servlet.http.HttpServletRequest request) {
        if (principal == null) return "redirect:/login";
        User u = userRepo.findByEmail(principal.getName()).orElse(null);
        
        if (u != null) {
            historyRepo.deleteAll(historyRepo.findByUser_UserIdOrderByUpdatedAtDesc(u.getUserId()));
            userRepo.delete(u);
            try {
                request.logout();
            } catch (jakarta.servlet.ServletException e) {
            }
        }
        return "redirect:/login?logout";
    }
}