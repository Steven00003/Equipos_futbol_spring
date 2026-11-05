package com.example.demo.app.dto;

import com.example.demo.app.entidades.Jugador;

public record JugadorDTO(Long id, String nombre, String apellido, int numero, String posicion) {

	public static JugadorDTO desde(Jugador j) {
		return new JugadorDTO(j.getId(), j.getNombre(), j.getApellido(), j.getNumero(), j.getPosicion());
	}
}
