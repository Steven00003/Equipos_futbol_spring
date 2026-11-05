package com.example.demo.app.entidades;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "clubes")
public class Club {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;

	// Uno a Uno: FK id_entrenador (UNIQUE) en la tabla clubes, sin tabla intermedia.
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_entrenador", unique = true)
	private Entrenador entrenador;

	// Uno a Muchos: FK id_club en la tabla jugadores, sin tabla intermedia.
	// Club es el dueño; CascadeType.ALL propaga el ciclo de vida de los jugadores.
	// @OnDelete: delega al motor de BD el borrado de los jugadores del club
	// (ON DELETE CASCADE en la FK jugadores.id_club).
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "id_club")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private List<Jugador> jugadores = new ArrayList<>();

	// Muchos a Uno: FK id_asociacion en la tabla clubes.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_asociacion")
	private Asociacion asociacion;

	// Muchos a Muchos: tabla intermedia clubes_competiciones con FKs id_club e id_competicion.
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "clubes_competiciones",
			joinColumns = @JoinColumn(name = "id_club"),
			inverseJoinColumns = @JoinColumn(name = "id_competicion"))
	private List<Competicion> competiciones = new ArrayList<>();

	public Club() {
	}

	public Club(String nombre) {
		this.nombre = nombre;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public Entrenador getEntrenador() {
		return entrenador;
	}

	public void setEntrenador(Entrenador entrenador) {
		this.entrenador = entrenador;
	}

	public List<Jugador> getJugadores() {
		return jugadores;
	}

	public void setJugadores(List<Jugador> jugadores) {
		this.jugadores = jugadores;
	}

	public void addJugador(Jugador jugador) {
		this.jugadores.add(jugador);
	}

	public Asociacion getAsociacion() {
		return asociacion;
	}

	public void setAsociacion(Asociacion asociacion) {
		this.asociacion = asociacion;
	}

	public List<Competicion> getCompeticiones() {
		return competiciones;
	}

	public void setCompeticiones(List<Competicion> competiciones) {
		this.competiciones = competiciones;
	}
}
