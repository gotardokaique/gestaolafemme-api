//package com.gestao.lafemme.api.controllers;
//
//import java.util.List;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.gestao.lafemme.api.controllers.dto.CompraMovimentacaoRequestDTO;
//import com.gestao.lafemme.api.controllers.dto.CompraRequestDTO;
//import com.gestao.lafemme.api.controllers.dto.CompraResponseDTO;
//import com.gestao.lafemme.api.controllers.dto.MovimentacaoEstoqueResponseDTO;
//import com.gestao.lafemme.api.entity.Compra;
//import com.gestao.lafemme.api.entity.MovimentacaoEstoque;
//import com.gestao.lafemme.api.services.CompraService;
//
//@RestController
//@RequestMapping("/api/v1/compras")
//public class CompraController {
//
//    private final CompraService compraService;
//
//    public CompraController(CompraService compraService) {
//        this.compraService = compraService;
//    }
//
//    @PostMapping
//    public ResponseEntity<Void> criar(@RequestBody CompraRequestDTO dto) throws Exception {
//
//        compraService.criarCompra(null, null, null, null, null);
//
//        return ResponseEntity.status(HttpStatus.CREATED).build();
//    }
//
//    @GetMapping
//    public ResponseEntity<List<CompraResponseDTO>> listar() {
//        List<Compra> lista = compraService.listarCompras();
//        return ResponseEntity.ok(lista.stream().map(CompraResponseDTO::from).toList());
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<CompraResponseDTO> buscarPorId(@PathVariable Long id) {
//        Compra c = compraService.buscarPorId(id);
//        return ResponseEntity.ok(CompraResponseDTO.from(c));
//    }
//
////    @PostMapping("/{id}/lancamento")
////    public ResponseEntity<Void> gerarLancamento(@PathVariable Long id) {
////        Compra compra = compraService.buscarPorId(id);
////        compraService.gerarLancamentoFinanceiro(compra);
////        return ResponseEntity.noContent().build();
////    }
////
////    @PostMapping("/{id}/movimentacoes")
////    public ResponseEntity<MovimentacaoEstoqueResponseDTO> adicionarMovimentacao(
////            @PathVariable Long id,
////            @RequestBody CompraMovimentacaoRequestDTO dto
////    ) {
////        MovimentacaoEstoque mov = compraService.adicionarMovimentacaoEntrada(
////                id,
////                dto.produtoId(),
////                dto.quantidade(),
////                dto.observacao()
////        );
////        return ResponseEntity.status(HttpStatus.CREATED).body(MovimentacaoEstoqueResponseDTO.from(mov));
////    }
//}
