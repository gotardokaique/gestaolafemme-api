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
//import com.gestao.lafemme.api.controllers.dto.MovimentacaoEstoqueResponseDTO;
//import com.gestao.lafemme.api.controllers.dto.VendaMovimentacaoRequestDTO;
//import com.gestao.lafemme.api.controllers.dto.VendaRequestDTO;
//import com.gestao.lafemme.api.controllers.dto.VendaResponseDTO;
//import com.gestao.lafemme.api.entity.MovimentacaoEstoque;
//import com.gestao.lafemme.api.entity.Venda;
//import com.gestao.lafemme.api.services.VendaService;
//
//@RestController
//@RequestMapping("/api/v1/vendas")
//public class VendaController {
//
//    private final VendaService vendaService;
//
//    public VendaController(VendaService vendaService) {
//        this.vendaService = vendaService;
//    }
//
//    @PostMapping
//    public ResponseEntity<VendaResponseDTO> criar(@RequestBody VendaRequestDTO dto) {
//        Venda criada = vendaService.criarVenda(dto.valorTotal(), dto.formaPagamento(), dto.observacao());
//        Venda completa = vendaService.buscarPorId(criada.getId());
//        return ResponseEntity.status(HttpStatus.CREATED).body(VendaResponseDTO.from(completa));
//    }
//
//    @GetMapping
//    public ResponseEntity<List<VendaResponseDTO>> listar() {
//        List<Venda> lista = vendaService.listarVendas();
//        return ResponseEntity.ok(lista.stream().map(VendaResponseDTO::from).toList());
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<VendaResponseDTO> buscarPorId(@PathVariable Long id) {
//        Venda v = vendaService.buscarPorId(id);
//        return ResponseEntity.ok(VendaResponseDTO.from(v));
//    }
//
//    @PostMapping("/{id}/lancamento")
//    public ResponseEntity<Void> gerarLancamento(@PathVariable Long id) {
//        Venda venda = vendaService.buscarPorId(id);
//        vendaService.gerarLancamentoFinanceiro(venda);
//        return ResponseEntity.noContent().build();
//    }
//
//    @PostMapping("/{id}/movimentacoes")
//    public ResponseEntity<MovimentacaoEstoqueResponseDTO> adicionarMovimentacao(
//            @PathVariable Long id,
//            @RequestBody VendaMovimentacaoRequestDTO dto
//    ) {
//        MovimentacaoEstoque mov = vendaService.adicionarMovimentacaoSaida(
//                id,
//                dto.produtoId(),
//                dto.quantidade(),
//                dto.observacao()
//        );
//        return ResponseEntity.status(HttpStatus.CREATED).body(MovimentacaoEstoqueResponseDTO.from(mov));
//    }
//}
