package tech.claudioed.payments.domain;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author claudioed on 2019-03-01.
 * Project payments
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRegisterRequest {

  private BigDecimal value;

  private String requesterId;

  private String customerId;

  private String token;

  private String orderId;

  private String crmId;

}
