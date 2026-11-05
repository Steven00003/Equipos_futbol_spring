package com.example.demo.app.dto;

import com.example.demo.app.entidades.Entrenador;

public record EntrenadorDTO(Long id, String nombre, String apellido, int edad, String nacionalidad) {

	public static EntrenadorDTO desde(Entrenador e) {
		return new EntrenadorDTO(e.getId(), e.getNombre(), e.getApellido(), e.getEdad(), e.getNacionalidad());
	}
}
