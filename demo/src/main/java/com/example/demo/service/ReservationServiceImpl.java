package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.Client;
import com.example.demo.entities.DiningTable;
import com.example.demo.entities.Reservation;
import com.example.demo.entities.ReservationTheme;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.DiningTableRepository;
import com.example.demo.repository.ReservationRepository;

@Service
public class ReservationServiceImpl implements ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DiningTableRepository diningTableRepository;

    @Override
    @Transactional
    public ReservationDTO getReservation(Integer clientId) {
        return toDTO(findOwned(clientId));
    }

    // Crea la reserva del cliente: solo puede tener una y debe aceptar el anticipo
    @Override
    @Transactional
    public ReservationDTO createReservation(Integer clientId, ReservationDTO dto) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> ApiException.notFound("No se encontró el cliente solicitado."));

        if (reservationRepository.existsByClientId(clientId)) {
            throw ApiException.conflict(
                    "Ya tienes una reserva. Edítala o elimínala para hacer otra.");
        }
        if (!Boolean.TRUE.equals(dto.getDepositAccepted())) {
            throw ApiException.badRequest(
                    "Debes aceptar el pago del anticipo para crear la reserva.");
        }

        String name = ReservationRules.cleanName(dto.getName());
        Integer seats = ReservationRules.cleanSeats(dto.getSeats());
        DiningTable table = findTable(dto.getTableId(), seats);
        ReservationRules.checkSchedule(dto.getDate(), dto.getTime());
        checkTableFree(table, dto.getDate(), dto.getTime(), null);

        Reservation reservation = Reservation.builder()
                .client(client)
                .name(name)
                .table(table)
                .date(dto.getDate())
                .time(dto.getTime())
                .seats(seats)
                .theme(dto.getTheme() != null ? dto.getTheme() : ReservationTheme.NONE)
                .build();
        // Se enlaza también por el lado del cliente para mantener el 1 a 1 coherente
        client.setReservation(reservation);
        return toDTO(reservationRepository.save(reservation));
    }

    // Cambia los datos de la reserva (el anticipo ya está pagado, no se vuelve a
    // pedir)
    @Override
    @Transactional
    public ReservationDTO updateReservation(Integer clientId, ReservationDTO dto) {
        Reservation reservation = findOwned(clientId);

        String name = ReservationRules.cleanName(dto.getName());
        Integer seats = ReservationRules.cleanSeats(dto.getSeats());
        DiningTable table = findTable(dto.getTableId(), seats);
        ReservationRules.checkSchedule(dto.getDate(), dto.getTime());
        checkTableFree(table, dto.getDate(), dto.getTime(), reservation.getId());

        // La entidad está gestionada: se guarda sola al terminar la transacción
        reservation.setName(name);
        reservation.setTable(table);
        reservation.setDate(dto.getDate());
        reservation.setTime(dto.getTime());
        reservation.setSeats(seats);
        reservation.setTheme(dto.getTheme() != null ? dto.getTheme() : ReservationTheme.NONE);
        return toDTO(reservation);
    }

    // Elimina la reserva. El anticipo NO se reembolsa.
    @Override
    @Transactional
    public void deleteReservation(Integer clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> ApiException.notFound("No se encontró el cliente solicitado."));
        Reservation reservation = findOwned(clientId);

        // orphanRemoval en Client.reservation la borra de la BD
        client.setReservation(null);
        reservationRepository.delete(reservation);
    }

    @Override
    @Transactional
    public List<Integer> getTakenTables(Integer clientId, LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            throw ApiException.badRequest("Elige la fecha y la hora.");
        }
        // La mesa del propio cliente no cuenta, así puede conservarla al editar
        return reservationRepository.findByDateAndTime(date, time).stream()
                .filter(r -> !r.getClient().getId().equals(clientId))
                .map(r -> r.getTable().getId())
                .toList();
    }

    private Reservation findOwned(Integer clientId) {
        return reservationRepository.findByClientId(clientId)
                .orElseThrow(() -> ApiException.notFound("Aún no tienes una reserva."));
    }

    // La mesa debe existir en el plano, estar en servicio y tener sillas suficientes
    private DiningTable findTable(Integer tableId, Integer seats) {
        if (tableId == null) {
            throw ApiException.badRequest("Elige una mesa.");
        }
        DiningTable table = diningTableRepository.findById(tableId)
                .orElseThrow(() -> ApiException.badRequest("La mesa no es válida."));
        if (!Boolean.TRUE.equals(table.getAvailable())) {
            throw ApiException.conflict("Esa mesa no está disponible.");
        }
        if (seats > table.getSeats()) {
            throw ApiException.badRequest(
                    "Esa mesa admite como máximo " + table.getSeats() + " sillas.");
        }
        return table;
    }

    // ignoreId: al editar, la propia reserva no cuenta como ocupada
    private void checkTableFree(DiningTable table, LocalDate date, LocalTime time, Integer ignoreId) {
        reservationRepository.findByTableIdAndDateAndTime(table.getId(), date, time)
                .filter(r -> !r.getId().equals(ignoreId))
                .ifPresent(r -> {
                    throw ApiException.conflict("Esa mesa ya está reservada en esa fecha y hora.");
                });
    }

    private ReservationDTO toDTO(Reservation r) {
        ReservationDTO dto = new ReservationDTO();
        dto.setId(r.getId());
        dto.setClientId(r.getClient().getId());
        dto.setName(r.getName());
        dto.setTableId(r.getTable().getId());
        dto.setFloor(r.getTable().getFloor());
        dto.setDate(r.getDate());
        dto.setTime(r.getTime());
        dto.setSeats(r.getSeats());
        dto.setTheme(r.getTheme());
        dto.setDepositAmount(ReservationRules.DEPOSIT_AMOUNT);
        return dto;
    }
}