package com.example.demo.app.dto;

import java.time.LocalDate;

public record CompeticionRequest(String nombre, int montoPremio, LocalDate fechaInicio, LocalDate fechaFin) {
}
