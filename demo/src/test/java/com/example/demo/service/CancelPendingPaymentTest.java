package com.example.demo.service;

import com.example.demo.entities.Client;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderStatus;
import com.example.demo.movil.service.OrderMirrorService;
import com.example.demo.repository.OrderRepository;
import com.stripe.StripeClient;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CancelPendingPaymentTest {
    private final PaymentService service = new PaymentService(mock(PlatformTransactionManager.class));
    private final StripeClient stripe = mock(StripeClient.class, RETURNS_DEEP_STUBS);
    private Order order;

    @BeforeEach
    void setup() {
        Client client = mock(Client.class);
        when(client.getId()).thenReturn(12);
        order = Order.builder().id(8).client(client).status(OrderStatus.PENDING)
                .paymentState("PENDING").paymentReference("tg:test").paymentAmount(50000L)
                .stripeSessionId("cs_test").stripeCheckoutUrl("checkout").build();
        OrderRepository orders = mock(OrderRepository.class);
        when(orders.lockById(8)).thenReturn(Optional.of(order));
        ReflectionTestUtils.setField(service, "orders", orders);
        ReflectionTestUtils.setField(service, "stripe", stripe);
        ReflectionTestUtils.setField(service, "mirror", mock(OrderMirrorService.class));
        ReflectionTestUtils.setField(service, "invoices", mock(InvoiceService.class));
        ReflectionTestUtils.setField(service, "events", mock(ApplicationEventPublisher.class));
    }

    private Session session(String status, String paymentStatus) {
        Session session = new Session();
        session.setId("cs_test");
        session.setStatus(status);
        session.setPaymentStatus(paymentStatus);
        session.setClientReferenceId("tg:test");
        session.setAmountTotal(50000L);
        session.setCurrency("cop");
        session.setMode("payment");
        session.setLivemode(false);
        return session;
    }

    @Test
    void repeatCancellationExpiresOneOpenSession() throws Exception {
        var sessions = stripe.v1().checkout().sessions();
        when(sessions.retrieve("cs_test")).thenReturn(session("open", "unpaid"));
        when(sessions.expire("cs_test")).thenReturn(session("expired", "unpaid"));

        assertThat(service.cancelPending(8, 12).state()).isEqualTo("EXPIRED");
        assertThat(service.cancelPending(8, 12).state()).isEqualTo("EXPIRED");
        verify(sessions, times(1)).expire("cs_test");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void paymentCompletedWhileExpiringIsKeptPaid() throws Exception {
        var sessions = stripe.v1().checkout().sessions();
        when(sessions.retrieve("cs_test")).thenReturn(session("open", "unpaid"), session("complete", "paid"));
        when(sessions.expire("cs_test")).thenThrow(mock(InvalidRequestException.class));

        assertThat(service.cancelPending(8, 12).state()).isEqualTo("PAID");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(stripe.v1(), never()).refunds();
    }

    @Test
    void completedSessionAwaitingSettlementCannotBeResumedOrExpired() throws Exception {
        var sessions = stripe.v1().checkout().sessions();
        when(sessions.retrieve("cs_test")).thenReturn(session("complete", "unpaid"));

        var result = service.cancelPending(8, 12);
        assertThat(result.state()).isEqualTo("PENDING");
        assertThat(result.checkoutUrl()).isNull();
        verify(sessions, never()).expire("cs_test");
    }
}
