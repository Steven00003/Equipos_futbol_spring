package com.example.demo.app.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.app.entidades.Entrenador;

public interface EntrenadorRepository extends JpaRepository<Entrenador, Long> {
}
