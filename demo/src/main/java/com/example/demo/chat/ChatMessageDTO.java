package com.example.demo.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un mensaje de la conversación. role es "user" (el cliente) o "model" (el asistente).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    private String role;

    private String text;
}