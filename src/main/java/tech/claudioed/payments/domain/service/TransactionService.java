package tech.claudioed.payments.domain.service;

import io.micrometer.core.instrument.Counter;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tech.claudioed.payments.domain.RegisteredPayment;
import tech.claudioed.payments.domain.Requester;
import tech.claudioed.payments.domain.Transaction;
import tech.claudioed.payments.domain.exception.TransactionErrorException;
import tech.claudioed.payments.domain.exception.TransactionNotFound;
import tech.claudioed.payments.domain.repository.TransactionRepository;
import tech.claudioed.payments.domain.resource.data.TransactionRequest;

/** @author claudioed on 2019-03-02. Project payments */
@Slf4j
@Service
public class TransactionService {

  private final TransactionRepository transactionRepository;

  private final CheckRequesterService checkRequesterService;

  private final RegisterPaymentService registerPaymentService;

  private final Counter transactionCounter;

  private final FraudService fraudService;

  public TransactionService(
      TransactionRepository transactionRepository,
      CheckRequesterService checkRequesterService,
      RegisterPaymentService registerPaymentService,
      @Qualifier("transactionCounter") Counter transactionCounter,
      FraudService fraudService) {
    this.transactionRepository = transactionRepository;
    this.checkRequesterService = checkRequesterService;
    this.registerPaymentService = registerPaymentService;
    this.transactionCounter = transactionCounter;
    this.fraudService = fraudService;
  }

  public Transaction processTransaction(@NonNull TransactionRequest request, String requesterId) {
    log.info("Processing transaction for order id : {} ", request.getOrderId());
    try {
      final Requester requester = this.checkRequesterService.requester(requesterId);
      final RegisteredPayment registeredPayment = this.registerPaymentService.registerPayment(request, requesterId);
      final Transaction transaction =
          Transaction.builder()
              .customerId(request.getCustomerId())
              .requesterId(requester.getId())
              .id(UUID.randomUUID().toString())
              .type(request.getType())
              .orderId(request.getOrderId())
              .paymentId(registeredPayment.getId())
              .value(request.getValue())
              .city(request.getCity())
              .build();
      transactionCounter.increment();
      log.info("New transaction created ID  : {}", transaction.getId());
      fraudService.analyzeTransaction(transaction);
      return this.transactionRepository.save(transaction);
    } catch (Exception ex) {
      log.error("Error on processing transaction " + request.toString(), ex);
      throw new TransactionErrorException("Invalid Transaction");
    }
  }

  public Transaction find(String id) {
    log.info("Finding transaction  : {}", id);
    final Optional<Transaction> transaction = this.transactionRepository.findById(id);
    if (transaction.isPresent()) {
      return transaction.get();
    }
    log.error("Transaction id {} not found ");
    throw new TransactionNotFound("Transaction not Found");
  }

}
