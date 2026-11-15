package com.example.demo.chat;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Lo que envía el front al chat.
@Getter
@Setter
@NoArgsConstructor
public class ChatRequestDTO {

    // Lo que el cliente acaba de escribir
    private String message;

    // Mensajes anteriores de esta conversación (Gemini no recuerda nada entre peticiones)
    private List<ChatMessageDTO> history = new ArrayList<>();
}