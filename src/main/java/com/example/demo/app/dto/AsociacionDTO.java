package com.example.demo.app.dto;

import com.example.demo.app.entidades.Asociacion;

public record AsociacionDTO(Long id, String nombre, String pais, String presidente) {

	public static AsociacionDTO desde(Asociacion a) {
		return new AsociacionDTO(a.getId(), a.getNombre(), a.getPais(), a.getPresidente());
	}
}
