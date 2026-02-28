package com.gestao.lafemme.api.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.Anexo;
import com.gestao.lafemme.api.entity.Produto;
import com.gestao.lafemme.api.enuns.TipoAnexo;
import com.gestao.lafemme.api.services.exceptions.NotFoundException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller para buscar previews de arquivos/fotos de diversas entidades.
 * SOMENTE LEITURA - operações de escrita devem ser feitas nos controllers específicos.
 * Base URL: /api/v1/file-preview
 */
@RestController
@RequestMapping("/api/v1/file-preview")
public class FilePreviewController {

    private final DAOController dao;

    public FilePreviewController(DAOController dao) {
        this.dao = dao;
    }

    /**
     * Busca a foto principal de um produto pelo ID.
     * GET /api/v1/file-preview/produtos/{id}
     */
    @GetMapping("/produtos/{id}")
    public ResponseEntity<?> getFotoProduto(@PathVariable Long id) {
        try {
            Anexo anexo = dao.select()
                    .from(Anexo.class)
                    .join("produto")
                    .join("produto.unidade")
                    .where("produto.id", Condicao.EQUAL, id)
                    .where("produto.unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .where("tipo", Condicao.EQUAL, TipoAnexo.FOTO_PRODUTO)
                    .one();

            return ResponseEntity.ok(buildAnexoResponse(anexo));

        } catch (NotFoundException e) {
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao buscar foto do produto: " + e.getMessage());
        }
    }

    /**
     * Lista todas as fotos do catálogo de um produto.
     * GET /api/v1/file-preview/produtos/{id}/catalogo
     */
    @GetMapping("/produtos/{id}/catalogo")
    public ResponseEntity<?> listarFotosCatalogo(@PathVariable Long id) {
        try {
            // Validar que o produto existe e pertence à unidade
            validarProduto(id);

            List<Anexo> anexos;
            try {
                anexos = dao.select()
                        .from(Anexo.class)
                        .join("produto")
                        .join("produto.unidade")
                        .where("produto.id", Condicao.EQUAL, id)
                        .where("produto.unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                        .where("tipo", Condicao.EQUAL, TipoAnexo.FOTO_CATALOGO_PRODUTO)
                        .orderBy("dataCadastro", false)
                        .list();
            } catch (NotFoundException e) {
                anexos = new ArrayList<>();
            }

            List<Map<String, Object>> response = new ArrayList<>();
            for (Anexo anexo : anexos) {
                Map<String, Object> item = buildAnexoResponse(anexo);
                item.put("id", anexo.getId());
                response.add(item);
            }

            return ResponseEntity.ok(response);

        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao listar fotos do catálogo: " + e.getMessage());
        }
    }

    /**
     * Busca a foto de um usuário pelo ID.
     * GET /api/v1/file-preview/usuarios/{id}
     */
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<?> getFotoUsuario(@PathVariable Long id) {
        try {
            Anexo anexo = dao.select()
                    .from(Anexo.class)
                    .join("usuario")
                    .join("usuario.unidades")
                    .join("usuario.unidades.unidade")
                    .where("usuario.id", Condicao.EQUAL, id)
                    .where("usuario.unidades.unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .where("tipo", Condicao.EQUAL, TipoAnexo.FOTO_PERFIL_USUARIO)
                    .one();

            return ResponseEntity.ok(buildAnexoResponse(anexo));

        } catch (NotFoundException e) {
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao buscar foto do usuário: " + e.getMessage());
        }
    }

    /**
     * Busca a foto de uma unidade pelo ID.
     * GET /api/v1/file-preview/unidades/{id}
     */
    @GetMapping("/unidades/{id}")
    public ResponseEntity<?> getFotoUnidade(@PathVariable Long id) {
        try {
            if (!UserContext.isUsuarioDaUnidade(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Anexo anexo = dao.select()
                    .from(Anexo.class)
                    .join("unidade")
                    .where("unidade.id", Condicao.EQUAL, id)
                    .where("tipo", Condicao.EQUAL, TipoAnexo.FOTO_UNIDADE)
                    .one();

            return ResponseEntity.ok(buildAnexoResponse(anexo));

        } catch (NotFoundException e) {
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao buscar foto da unidade: " + e.getMessage());
        }
    }

    // ============ Métodos Auxiliares ============

    private Map<String, Object> buildAnexoResponse(Anexo anexo) {
        Map<String, Object> response = new HashMap<>();
        response.put("nome", anexo.getNome());
        response.put("mimeType", anexo.getMimeType());
        response.put("arquivo", Base64.getEncoder().encodeToString(anexo.getArquivo()));
        response.put("tamanhoBytes", anexo.getTamanhoBytes());
        return response;
    }

    private Produto validarProduto(Long id) throws Exception {
        return dao.select()
                .from(Produto.class)
                .join("unidade")
                .where("id", Condicao.EQUAL, id)
                .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                .one();
    }
}
