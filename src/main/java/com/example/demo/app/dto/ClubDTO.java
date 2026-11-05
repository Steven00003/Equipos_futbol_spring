package com.example.demo.app.dto;

import java.util.List;

import com.example.demo.app.entidades.Club;

public record ClubDTO(Long id, String nombre, EntrenadorDTO entrenador, AsociacionDTO asociacion,
		List<JugadorDTO> jugadores, List<CompeticionDTO> competiciones) {

	// Se construye SOLO dentro de un método @Transactional del controlador:
	// aquí se navegan las relaciones LAZY del Club.
	public static ClubDTO desde(Club club) {
		return new ClubDTO(club.getId(), club.getNombre(),
				club.getEntrenador() == null ? null : EntrenadorDTO.desde(club.getEntrenador()),
				club.getAsociacion() == null ? null : AsociacionDTO.desde(club.getAsociacion()),
				club.getJugadores().stream().map(JugadorDTO::desde).toList(),
				club.getCompeticiones().stream().map(CompeticionDTO::desde).toList());
	}
}
