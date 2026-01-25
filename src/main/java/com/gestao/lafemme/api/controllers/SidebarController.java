package com.gestao.lafemme.api.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.entity.Usuario;

@RestController
@RequestMapping("/api/v1/sidebar")
public class SidebarController {

    @GetMapping
    public ResponseEntity<SidebarResponse> getSidebar() {
        Usuario usuario = UserContext.getUsuarioAutenticado();

        UserDTO user = new UserDTO(
                usuario.getNome(),
                usuario.getEmail(),
                ""
        );

        List<TeamDTO> teams = List.of(
                new TeamDTO("La Femme Pratas", "UserRoundCheck", "Desde de 2026")
        );

        List<NavMainDTO> navMain = List.of(
                new NavMainDTO("Pessoas", "#", "UserStar", true, List.of(
                        new NavItemDTO("Fornecedores", "#"),
                        new NavItemDTO("Clientes", "#"),
                        new NavItemDTO("Settings", "#")
                )),
                new NavMainDTO("Itens", "#", "AudioWaveform", false, List.of(
                        new NavItemDTO("Estoque", "#"),
                        new NavItemDTO("Produtos", "#")
                )),
                new NavMainDTO("Financeiro", "#", "BookOpen", false, List.of(
                        new NavItemDTO("Compras", "#"),
                        new NavItemDTO("Vendas", "#"),
                        new NavItemDTO("Dashboard", "#"),
                        new NavItemDTO("Movimentações do Estoque", "#")
                )),
                new NavMainDTO("Configurações", "#", "Settings2", false, List.of(
                        new NavItemDTO("General", "#"),
                        new NavItemDTO("Team", "#"),
                        new NavItemDTO("Billing", "#"),
                        new NavItemDTO("Limits", "#")
                ))
        );

        List<ProjectDTO> projects = List.of(
                new ProjectDTO("Design Engineering", "#", "Frame"),
                new ProjectDTO("Sales & Marketing", "#", "PieChart"),
                new ProjectDTO("Travel", "#", "Map")
        );

        return ResponseEntity.ok(new SidebarResponse(user, teams, navMain, projects));
    }

    public record SidebarResponse(
            UserDTO user,
            List<TeamDTO> teams,
            List<NavMainDTO> navMain,
            List<ProjectDTO> projects
    ) {}

    public record UserDTO(String name, String email, String avatar) {}

    public record TeamDTO(String name, String logo, String plan) {}

    public record NavMainDTO(
            String title,
            String url,
            String icon,
            Boolean isActive,
            List<NavItemDTO> items
    ) {}

    public record NavItemDTO(String title, String url) {}

    public record ProjectDTO(String name, String url, String icon) {}
}
