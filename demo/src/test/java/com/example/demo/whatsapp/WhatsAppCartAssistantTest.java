package com.example.demo.whatsapp;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.example.demo.chat.GeminiClient;
import com.example.demo.errors.ApiException;
import com.google.common.collect.ImmutableList;
import com.google.genai.types.*;

class WhatsAppCartAssistantTest {
    GeminiClient gemini = mock(GeminiClient.class);
    WhatsAppAssistant publicAssistant = mock(WhatsAppAssistant.class);
    WhatsAppCartService cart = mock(WhatsAppCartService.class);
    WhatsAppCartAssistant assistant = new WhatsAppCartAssistant(gemini, publicAssistant, cart, true);
    WhatsAppCartService.Context context = new WhatsAppCartService.Context("inbound-id", "573001234567", new WhatsAppAccountService.Account(7, Instant.EPOCH));

    GenerateContentResponse calls(FunctionCall... calls) {
        var response = mock(GenerateContentResponse.class);
        when(response.functionCalls()).thenReturn(ImmutableList.copyOf(calls));
        when(response.candidates()).thenReturn(Optional.of(ImmutableList.of(Candidate.builder()
                .content(Content.builder().role("model").parts(Arrays.stream(calls).map(c -> Part.builder().functionCall(c).build()).toList()).build()).build())));
        return response;
    }

    @Test void exposesOnlyReadAndProposalToolsAndNeverAppliesCartChanges() {
        when(publicAssistant.cartPrompt()).thenReturn("Carrito vinculado, cambios solo con confirmación");
        var args = Map.<String, Object>of("accion", "agregar", "productoId", 12, "cantidad", 2);
        var response = calls(FunctionCall.builder().name("preparar_cambio_carrito").args(args).build());
        when(gemini.generate(anyList(), any())).thenReturn(response);
        when(cart.prepare(context, args)).thenReturn(new WhatsAppCartService.Reply("¿Confirmas agregar dos platos?", "a".repeat(64)));
        assertThat(assistant.answer(context, "Agrega 2 pulpos", List.of()).actionId()).isEqualTo("a".repeat(64));
        var config = ArgumentCaptor.forClass(GenerateContentConfig.class); verify(gemini).generate(anyList(), config.capture());
        assertThat(config.getValue().tools().orElseThrow().getFirst().functionDeclarations().orElseThrow())
                .extracting(f -> f.name().orElse("")).containsExactly("ver_carrito", "preparar_cambio_carrito");
        verify(cart).prepare(context, args); verifyNoMoreInteractions(cart);
    }

    @Test void resolvesLineReferencesFromRealCartBeforeProposingRemoval() {
        when(publicAssistant.cartPrompt()).thenReturn("Carrito");
        var args = Map.<String, Object>of("accion", "quitar", "lineaId", "12:5");
        var read = calls(FunctionCall.builder().name("ver_carrito").build());
        var change = calls(FunctionCall.builder().name("preparar_cambio_carrito").args(args).build());
        when(gemini.generate(anyList(), any())).thenReturn(read, change);
        when(cart.inspect(context)).thenReturn(Map.of("resumen", "Pulpo con adicional", "lineas", List.of(Map.of("lineaId", "12:5"))));
        when(cart.prepare(context, args)).thenReturn(new WhatsAppCartService.Reply("Quitar pulpo con adicional", "b".repeat(64)));
        assertThat(assistant.answer(context, "Quita el pulpo con adicional", List.of()).text()).contains("adicional");
        var contents = ArgumentCaptor.forClass(List.class); verify(gemini, times(2)).generate(contents.capture(), any());
        assertThat(contents.getValue().toString()).contains("12:5");
        verify(cart).inspect(context); verify(cart).prepare(context, args); verifyNoMoreInteractions(cart);
    }

    @Test void multipleMutationsAndUnknownToolsHaveNoPartialEffects() {
        when(publicAssistant.cartPrompt()).thenReturn("Carrito");
        var proposal = FunctionCall.builder().name("preparar_cambio_carrito").args(Map.of()).build();
        var multiple = calls(proposal, proposal);
        when(gemini.generate(anyList(), any())).thenReturn(multiple);
        assertThat(assistant.answer(context, "Agrega uno y quita otro", List.of()).text()).contains("a la vez");
        var forbidden = calls(FunctionCall.builder().name("crear_reserva").build());
        when(gemini.generate(anyList(), any())).thenReturn(forbidden);
        assertThat(assistant.answer(context, "Crea una reserva", List.of()).text()).contains("web");
        verifyNoInteractions(cart);
    }

    @Test void rejectedAuthorizationAndProviderErrorsNeverClaimSuccess() {
        when(publicAssistant.cartPrompt()).thenReturn("Carrito");
        var response = calls(FunctionCall.builder().name("preparar_cambio_carrito").args(Map.of()).build());
        when(gemini.generate(anyList(), any())).thenReturn(response);
        when(cart.prepare(context, Map.of())).thenThrow(ApiException.forbidden(WhatsAppAccountService.LINK_REQUIRED));
        assertThat(assistant.answer(context, "Agrega 2", List.of()).text()).isEqualTo(WhatsAppAccountService.LINK_REQUIRED);
        when(gemini.generate(anyList(), any())).thenThrow(new IllegalStateException("provider down"));
        assertThat(assistant.answer(context, "Agrega 2", List.of()).text()).isEqualTo(WhatsAppAssistant.UNAVAILABLE);
    }

    @Test void disabledAndOversizedMessagesDoNotCallGeminiOrCart() {
        assertThat(assistant.answer(context, "x".repeat(501), List.of()).text()).contains("500");
        assertThat(new WhatsAppCartAssistant(gemini, publicAssistant, cart, false).answer(context, "Agrega 2", List.of()).text()).isEqualTo(WhatsAppMessageClient.TEST_REPLY);
        verifyNoInteractions(gemini, cart);
    }
}
