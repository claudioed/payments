package tech.claudioed.payments.domain.resource;

import io.micrometer.core.annotation.Timed;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import tech.claudioed.payments.domain.Transaction;
import tech.claudioed.payments.domain.resource.data.TransactionRequest;
import tech.claudioed.payments.domain.service.TransactionService;

/** @author claudioed on 2019-03-02. Project payments */
@RestController
@RequestMapping("/api/transactions")
public class TransactionResource {

  private final TransactionService transactionService;

  public TransactionResource(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @PostMapping
  @Timed(value = "transaction.time.seconds")
  public ResponseEntity<Transaction> newTransaction(
      @RequestBody TransactionRequest transactionRequest,
      @RequestHeader("requester-id") String requesterId,
      UriComponentsBuilder uriBuilder) {
    try {
      final Transaction transaction =
          this.transactionService.processTransaction(transactionRequest, requesterId);
      final UriComponents uriComponents =
          uriBuilder.path("api/transactions/{id}").buildAndExpand(transaction.getId());
      return ResponseEntity.created(uriComponents.toUri()).body(transaction);
    } catch (Exception ex) {
      return ResponseEntity.unprocessableEntity().build();
    }
  }

  @GetMapping("/{id}")
  @Timed(value = "transaction.find.time.seconds")
  public ResponseEntity<Transaction> find(@PathVariable("id") String id) {
    try {
      final Transaction transaction = this.transactionService.find(id);
      return ResponseEntity.ok(transaction);
    } catch (Exception ex) {
      return ResponseEntity.notFound().build();
    }
  }
}
