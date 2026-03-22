package com.alexis.paymybuddy.Service;

import com.alexis.paymybuddy.DTO.TransactionRequestDTO;
import com.alexis.paymybuddy.Model.Transaction;
import com.alexis.paymybuddy.Model.User;
import com.alexis.paymybuddy.Repository.TransactionRepository;
import com.alexis.paymybuddy.Repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User sender;
    private User receiver;

    @BeforeEach
    void setup() {
        sender = new User("Alice", "alice@example.com", "pass");
        sender.setId(1L);
        sender.setActive(true);
        sender.setBalance(new BigDecimal("100.00"));

        receiver = new User("Bob", "bob@example.com", "pass");
        receiver.setId(2L);
        receiver.setActive(true);
        receiver.setBalance(new BigDecimal("50.00"));
    }

    @Test
    void valid_transaction() {

        sender.getConnections().add(receiver);

        TransactionRequestDTO dto =
                new TransactionRequestDTO(1L, 2L, new BigDecimal("30.00"), "Cadeau");

        when(userRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(sender));

        when(userRepository.findByIdAndActiveTrue(2L))
                .thenReturn(Optional.of(receiver));

        transactionService.processTransaction(1L, dto);

        assertEquals(new BigDecimal("70.00"), sender.getBalance());
        assertEquals(new BigDecimal("80.00"), receiver.getBalance());

        verify(transactionRepository).save(any(Transaction.class));
        verify(userRepository).save(sender);
        verify(userRepository).save(receiver);
    }

    @Test
    void transaction_not_in_connections() {

        TransactionRequestDTO dto =
                new TransactionRequestDTO(1L, 2L, new BigDecimal("10.00"), "Essai");

        when(userRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(sender));

        when(userRepository.findByIdAndActiveTrue(2L))
                .thenReturn(Optional.of(receiver));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.processTransaction(1L, dto)
        );

        assertEquals("Le destinataire n’est pas dans vos connexions.", ex.getMessage());

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transaction_insufficient_balance() {

        sender.setBalance(new BigDecimal("5.00"));
        sender.getConnections().add(receiver);

        TransactionRequestDTO dto =
                new TransactionRequestDTO(1L, 2L, new BigDecimal("10.00"), "Essai");

        when(userRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(sender));

        when(userRepository.findByIdAndActiveTrue(2L))
                .thenReturn(Optional.of(receiver));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.processTransaction(1L, dto)
        );

        assertEquals("Solde insuffisant", ex.getMessage());

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transaction_invalid_amount() {

        sender.getConnections().add(receiver);

        TransactionRequestDTO dto =
                new TransactionRequestDTO(1L, 2L, BigDecimal.ZERO, "Essai");

        when(userRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(sender));

        when(userRepository.findByIdAndActiveTrue(2L))
                .thenReturn(Optional.of(receiver));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.processTransaction(1L, dto)
        );

        assertEquals("Montant invalide", ex.getMessage());

        // ✅ AJOUT IMPORTANT
        verify(userRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transaction_sender_not_found() {

        TransactionRequestDTO dto =
                new TransactionRequestDTO(1L, 2L, new BigDecimal("20.00"), "Test");

        when(userRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.processTransaction(1L, dto)
        );

        assertEquals("Expéditeur introuvable", ex.getMessage());

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transaction_receiver_not_found() {

        TransactionRequestDTO dto =
                new TransactionRequestDTO(1L, 2L, new BigDecimal("20.00"), "Test");

        when(userRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(sender));

        when(userRepository.findByIdAndActiveTrue(2L))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.processTransaction(1L, dto)
        );

        assertEquals("Destinataire introuvable ou inactif", ex.getMessage());

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transaction_to_self_not_allowed() {

        // Le sender se “connecte” à lui-même pour que le test soit cohérent
        sender.getConnections().add(sender);

        TransactionRequestDTO dto =
                new TransactionRequestDTO(1L, 1L, new BigDecimal("10.00"), "Test");

        // Mock sender
        when(userRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(sender));

        // Mock receiver (même objet que sender)
        when(userRepository.findByIdAndActiveTrue(dto.getReceiverId()))
                .thenReturn(Optional.of(sender));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.processTransaction(1L, dto)
        );

        assertEquals("Impossible de se transférer de l'argent à soi-même", ex.getMessage());
    } }