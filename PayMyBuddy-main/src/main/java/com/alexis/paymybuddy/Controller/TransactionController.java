package com.alexis.paymybuddy.Controller;

import com.alexis.paymybuddy.DTO.TransactionRequestDTO;
import com.alexis.paymybuddy.Service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/users/{id}/transactions")
    public ResponseEntity<?> makeTransaction(@PathVariable Long id, @RequestBody TransactionRequestDTO dto) {
        if (dto.getReceiverId() == null || dto.getAmount() == null) {
            return ResponseEntity.badRequest().body("Le destinataire et le montant sont obligatoires");
        }

        try {
            transactionService.processTransaction(id, dto);
            return ResponseEntity.ok("Transaction effectuée avec succès.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
