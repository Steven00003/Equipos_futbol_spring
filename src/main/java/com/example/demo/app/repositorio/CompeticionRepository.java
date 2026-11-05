package com.example.demo.app.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.app.entidades.Competicion;

public interface CompeticionRepository extends JpaRepository<Competicion, Long> {
}
