package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.dto.AddOnDTO;
import com.example.demo.entities.AddOn;
import com.example.demo.errors.ApiException;
import com.example.demo.service.AddOnService;

import tools.jackson.databind.ObjectMapper;

/*
    Pruebas del AddOnController simulando peticiones HTTP con MockMvc.
 
 */
@WebMvcTest(AddOnController.class)
class AddOnControllerTest {

    // Simula las peticiones HTTP sin levantar un servidor real
    @Autowired
    private MockMvc mockMvc;

    // Pasa un objeto a su representación JSON para enviarlo en el body
    @Autowired
    private ObjectMapper objectMapper;

    // Servicio simulado: no hay base de datos ni lógica de negocio real
    @MockitoBean
    private AddOnService addOnService;

    // Constructor de un adicional (entidad) como lo devolvería el servicio
    private AddOn buildAddOn(Integer id, String name, Double price) {
        return AddOn.builder()
                .id(id)
                .name(name)
                .description("Adicional de prueba")
                .price(price)
                .active(true)
                .build();
    }

    // Constructor del cuerpo (DTO) que el front enviaría al crear o actualizar
    private AddOnDTO buildDto(String name, Double price) {
        AddOnDTO dto = new AddOnDTO();
        dto.setName(name);
        dto.setDescription("Adicional de prueba");
        dto.setPrice(price);
        dto.setActive(true);
        return dto;
    }

    @Test
    void AddOnController_GetAddOnById_ReturnsAddOnJson() throws Exception {
        // Arrange: el servicio devuelve el adicional con id 1
        when(addOnService.getAddOnById(1)).thenReturn(buildAddOn(1, "Queso extra", 3000.0));

        // Act
        var response = mockMvc.perform(get("/addons/1"));

        // Assert: 200 y el JSON con los datos del adicional
        response.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Queso extra"))
                .andExpect(jsonPath("$.price").value(3000.0))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void AddOnController_AddAddOn_ReturnsCreatedAddOnJson() throws Exception {
        // Arrange: el front envía un adicional sin id; el servicio lo "guarda" con id 7
        AddOnDTO body = buildDto("Salsa brava", 1500.0);
        when(addOnService.addAddOn(any(AddOn.class)))
                .thenReturn(buildAddOn(7, "Salsa brava", 1500.0));

        // Act: POST con el DTO convertido a JSON
        var response = mockMvc.perform(post("/addons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));

        // Assert: la respuesta trae el adicional ya con su id
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Salsa brava"))
                .andExpect(jsonPath("$.price").value(1500.0));
    }

    @Test
    void AddOnController_UpdateAddOn_ReturnsUpdatedAddOnJson() throws Exception {
        // Arrange: se cambia el nombre y el precio del adicional 1
        AddOnDTO body = buildDto("Queso manchego", 3500.0);
        when(addOnService.updateAddOn(eq(1), any(AddOn.class)))
                .thenReturn(buildAddOn(1, "Queso manchego", 3500.0));

        // Act
        var response = mockMvc.perform(put("/addons/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));

        // Assert: la respuesta refleja los datos nuevos
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Queso manchego"))
                .andExpect(jsonPath("$.price").value(3500.0));
    }

    @Test
    void AddOnController_DeleteAddOn_ReturnsOkWithEmptyBody() throws Exception {
        // Arrange: deleteAddOn no devuelve nada, así que el mock no necesita configuración

        // Act
        var response = mockMvc.perform(delete("/addons/1"));

        // Assert: 200 y sin contenido en la respuesta
        response.andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void AddOnController_GetAddOnById_WhenNotFound_ReturnsNotFoundError() throws Exception {
        // Arrange: el servicio lanza el error de negocio "no existe"
        when(addOnService.getAddOnById(99))
                .thenThrow(ApiException.notFound("No se encontró el adicional solicitado."));

        // Act
        var response = mockMvc.perform(get("/addons/99"));

        // Assert: el GlobalExceptionHandler lo convierte en un 404 con el JSON de error
        response.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No se encontró el adicional solicitado."))
                .andExpect(jsonPath("$.path").value("/addons/99"));
    }
}