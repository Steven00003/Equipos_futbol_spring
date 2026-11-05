package com.example.demo.app.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.app.entidades.Asociacion;

public interface AsociacionRepository extends JpaRepository<Asociacion, Long> {
}
