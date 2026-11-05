package com.example.demo.app.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.app.entidades.Jugador;

public interface JugadorRepository extends JpaRepository<Jugador, Long> {
}
