package com.example.demo.app.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.app.entidades.Club;

public interface ClubRepository extends JpaRepository<Club, Long> {
}
