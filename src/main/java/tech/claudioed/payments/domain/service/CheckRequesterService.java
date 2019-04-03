package tech.claudioed.payments.domain.service;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.opentracing.Span;
import io.opentracing.Tracer;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tech.claudioed.payments.domain.Requester;
import tech.claudioed.payments.domain.exception.InvalidRequester;

/** @author claudioed on 2019-03-02. Project payments */
@Slf4j
@Service
public class CheckRequesterService {

  private final RestTemplate restTemplate;

  private final String requesterSvcUrl;

  private final Counter requesterCounter;

  private final Tracer tracer;

  public CheckRequesterService(
      RestTemplate restTemplate,
      @Value("${requester.service.url}") String requesterSvcUrl,
      @Qualifier("requesterCounter") Counter requesterCounter, Tracer tracer) {
    this.tracer = tracer;
    log.info("REQUESTER SERVICE URL: {}", requesterSvcUrl);
    this.restTemplate = restTemplate;
    this.requesterSvcUrl = requesterSvcUrl;
    this.requesterCounter = requesterCounter;
  }

  @Timed(value = "transaction.requester.time.seconds")
  public Requester requester(@NonNull String id) {
    log.info("Checking Requester ID : {}", id);
    final String path = requesterSvcUrl + "/api/requesters/{id}";
    try {
      final HttpHeaders headers = new HttpHeaders();
      headers.set("requester-id", id);
      final Span span = this.tracer.buildSpan("checking-requester").start();
      final ResponseEntity<Requester> entity = this.restTemplate
          .exchange(path, HttpMethod.GET, new HttpEntity<>(headers),
              Requester.class, id);
      Map<String, String> logs = Stream.of(new String[][] {
          { "requester-id", id },
          { "status", "valid" },
      }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
      span.log(logs).finish();
      requesterCounter.increment();
      log.info("Requester ID : {} is valid", id);
      return entity.getBody();
    } catch (Exception ex) {
      log.error("Invalid Requester " + id, ex);
      throw new InvalidRequester("Invalid Requester");
    }
  }
}
