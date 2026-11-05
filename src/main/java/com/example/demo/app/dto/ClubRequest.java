package com.example.demo.app.dto;

import java.util.List;

public record ClubRequest(String nombre, Long idEntrenador, Long idAsociacion, List<Long> idsCompeticiones) {
}
