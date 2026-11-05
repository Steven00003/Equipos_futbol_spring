package com.example.demo.app.dto;

import java.time.LocalDate;

import com.example.demo.app.entidades.Competicion;

public record CompeticionDTO(Long id, String nombre, int montoPremio, LocalDate fechaInicio, LocalDate fechaFin) {

	public static CompeticionDTO desde(Competicion c) {
		return new CompeticionDTO(c.getId(), c.getNombre(), c.getMontoPremio(), c.getFechaInicio(), c.getFechaFin());
	}
}
