package com.example.demo;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entities.DiningTable;
import com.example.demo.repository.DiningTableRepository;

// Carga las mesas del restaurante al arrancar, en todos los perfiles. Son las
// mismas del plano SVG de la página de reservas
// (frontend: src/app/pages/reservations/reservations-layout.ts); si se mueve,
// añade o quita una mesa allí, hay que cambiarla también aquí.
// Solo inserta las que faltan, así no duplica nada en bases que no se recrean.
@Component
@Order(0)
public class DiningTableLoader implements CommandLineRunner {

    // Mesas de arco: máximo 5 personas
    private static final int BOOTH = 5;
    // Mesas de celebración: se reservan de 7 a 20 sillas, se guarda el máximo
    private static final int CELEBRATION = 20;

    @Autowired
    private DiningTableRepository diningTableRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<DiningTable> missing = catalog().stream()
                .filter(t -> !diningTableRepository.existsById(t.getId()))
                .toList();
        diningTableRepository.saveAll(missing);
    }

    private static List<DiningTable> catalog() {
        List<DiningTable> tables = new ArrayList<>();

        // Piso 1
        add(tables, 1, 2, 101, 102, 103, 104, 105, 106); // ventanales
        add(tables, 1, 4, 107, 108, 109, 110, 111); // ventanales
        add(tables, 1, BOOTH, 112, 116); // salón principal, de arco
        add(tables, 1, 2, 113, 114, 115);
        add(tables, 1, 4, 117, 118, 119, 120);
        add(tables, 1, 2, 121, 122, 123); // salón central
        add(tables, 1, BOOTH, 124);
        add(tables, 1, 4, 125);
        add(tables, 1, BOOTH, 126, 128, 130); // salón lateral
        add(tables, 1, 6, 127, 129);

        // Piso 2
        add(tables, 2, 2, 201, 202, 203, 204, 205, 206, 207); // balcón
        add(tables, 2, 4, 208, 209, 210, 211); // balcón
        add(tables, 2, 2, 212, 213, 214, 215); // salón alto
        add(tables, 2, BOOTH, 216, 217, 218);
        add(tables, 2, 4, 219, 220);
        add(tables, 2, BOOTH, 221, 224, 226, 228, 230); // galería, de arco
        add(tables, 2, 4, 222, 223);
        add(tables, 2, 6, 225, 227, 229);
        add(tables, 2, CELEBRATION, 231, 232); // salas de celebración 1 y 2

        return tables;
    }

    private static void add(List<DiningTable> tables, int floor, int seats, int... numbers) {
        for (int number : numbers) {
            tables.add(DiningTable.builder()
                    .id(number)
                    .floor(floor)
                    .available(true)
                    .seats(seats)
                    .build());
        }
    }
}
