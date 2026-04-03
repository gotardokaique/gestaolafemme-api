package com.gestao.lafemme.api.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestao.lafemme.api.controllers.dto.AnexoFotoDTO;
import com.gestao.lafemme.api.controllers.dto.PublicCategoriaDTO;
import com.gestao.lafemme.api.controllers.dto.PublicProdutoDTO;
import com.gestao.lafemme.api.services.PublicCatalogService;

@RestController
@RequestMapping("/public/catalogo")
public class PublicCatalogController {

    private final PublicCatalogService publicCatalogService;

    public PublicCatalogController(PublicCatalogService publicCatalogService) {
        this.publicCatalogService = publicCatalogService;
    }

    @GetMapping("/produtos")
    public ResponseEntity<List<PublicProdutoDTO>> listarProdutos(@RequestHeader(value = "X-Api-Key", required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            List<PublicProdutoDTO> produtos = publicCatalogService.listarProdutos(token);
            return ResponseEntity.ok(produtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/produtos/{id}")
    public ResponseEntity<PublicProdutoDTO> buscarProduto(
            @RequestHeader(value = "X-Api-Key", required = false) String token,
            @PathVariable Long id) throws Exception {
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            PublicProdutoDTO produto = publicCatalogService.buscarProduto(token, id);
            if (produto == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(produto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping(value = "/produtos/{id}/foto", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> buscarFoto(
            @RequestHeader(value = "X-Api-Key", required = false) String token,
            @PathVariable Long id) {

        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            AnexoFotoDTO foto = publicCatalogService.buscarFoto(token, id);
            if (foto == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(foto.mimeType()))
                    .body(foto.arquivo());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<PublicCategoriaDTO>> listarCategorias(@RequestHeader(value = "X-Api-Key", required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            List<PublicCategoriaDTO> categorias = publicCatalogService.listarCategorias(token);
            return ResponseEntity.ok(categorias);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
