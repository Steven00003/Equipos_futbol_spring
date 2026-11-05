package com.example.demo.app.config;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.app.entidades.Asociacion;
import com.example.demo.app.entidades.Club;
import com.example.demo.app.entidades.Competicion;
import com.example.demo.app.entidades.Entrenador;
import com.example.demo.app.entidades.Jugador;
import com.example.demo.app.repositorio.AsociacionRepository;
import com.example.demo.app.repositorio.ClubRepository;
import com.example.demo.app.repositorio.CompeticionRepository;
import com.example.demo.app.repositorio.EntrenadorRepository;

/**
 * Inserta datos de prueba una sola vez, cuando la tabla clubes está vacía.
 * @Transactional: todo en una transacción evita LazyInitializationException
 * al navegar las relaciones LAZY al construir los datos base.
 */
@Component
public class DatosPruebaRunner implements CommandLineRunner {

	private final ClubRepository clubRepository;
	private final AsociacionRepository asociacionRepository;
	private final CompeticionRepository competicionRepository;
	private final EntrenadorRepository entrenadorRepository;

	public DatosPruebaRunner(ClubRepository clubRepository,
			AsociacionRepository asociacionRepository,
			CompeticionRepository competicionRepository,
			EntrenadorRepository entrenadorRepository) {
		this.clubRepository = clubRepository;
		this.asociacionRepository = asociacionRepository;
		this.competicionRepository = competicionRepository;
		this.entrenadorRepository = entrenadorRepository;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (clubRepository.count() > 0) {
			return; // ya hay datos: no repetir
		}

		// 2 asociaciones
		Asociacion fcf = new Asociacion("FCF", "Colombia", "Ramón Jesurún");
		Asociacion conmebol = new Asociacion("CONMEBOL", "Paraguay", "Alejandro Domínguez");
		asociacionRepository.save(fcf);
		asociacionRepository.save(conmebol);

		// 3 competiciones (se guardan primero: @ManyToMany no tiene cascade)
		Competicion liga = new Competicion("Liga BetPlay", 15000000,
				LocalDate.of(2026, 2, 1), LocalDate.of(2026, 11, 15));
		Competicion copa = new Competicion("Copa Colombia", 5000000,
				LocalDate.of(2026, 6, 10), LocalDate.of(2026, 10, 20));
		Competicion libertadores = new Competicion("Copa Libertadores", 40000000,
				LocalDate.of(2026, 4, 1), LocalDate.of(2026, 11, 30));
		competicionRepository.save(liga);
		competicionRepository.save(copa);
		competicionRepository.save(libertadores);

		// 3 clubes, cada uno con su entrenador, entrenadores guardados primero
		Entrenador e1 = new Entrenador("David", "González", 45, "Colombiano");
		Entrenador e2 = new Entrenador("Fabian", "Bustos", 52, "Colombiano");
		Entrenador e3 = new Entrenador("Alfredo", "Aguilar", 58, "Paraguayo");
		entrenadorRepository.save(e1);
		entrenadorRepository.save(e2);
		entrenadorRepository.save(e3);

		// 4 jugadores por club (los inserta la FK cascade del @OneToMany al guardar cada club)
		Club club1 = crearClub("Millonarios", e1, fcf,
				new Jugador("Juan", "Pérez", 1, "Portero"),
				new Jugador("Carlos", "Gómez", 10, "Mediocampista"),
				new Jugador("Andrés", "Mora", 9, "Delantero"),
				new Jugador("Camilo", "Torres", 4, "Defensa"));
		club1.getCompeticiones().add(liga);
		club1.getCompeticiones().add(copa);
		club1.getCompeticiones().add(libertadores);

		Club club2 = crearClub("Santa Fe", e2, conmebol,
				new Jugador("Pedro", "Ramírez", 1, "Portero"),
				new Jugador("Santiago", "Ospina", 8, "Mediocampista"),
				new Jugador("Tomás", "Salazar", 9, "Delantero"),
				new Jugador("Sebastián", "Ríos", 5, "Defensa"));
		club2.getCompeticiones().add(liga);
		club2.getCompeticiones().add(copa);
		club2.getCompeticiones().add(libertadores);

		Club club3 = crearClub("Nacional", e3, fcf,
				new Jugador("Felipe", "Restrepo", 1, "Portero"),
				new Jugador("Julián", "Ortiz", 7, "Extremo"),
				new Jugador("Mateo", "Vélez", 9, "Delantero"),
				new Jugador("Mauricio", "Peña", 6, "Defensa"));
		club3.getCompeticiones().add(liga);
		club3.getCompeticiones().add(libertadores);

		clubRepository.save(club1);
		clubRepository.save(club2);
		clubRepository.save(club3);

		System.out.println("Datos de prueba insertados en futbol_db");
	}

	// Helper privado: crea el club con entrenador, asociación y jugadores.
	// La FK jugadores.id_club la llena Hibernate (cascade ALL en el @OneToMany)
	// y clubes_competiciones se llena con las adiciones previas al save.
	private Club crearClub(String nombre, Entrenador entrenador, Asociacion asociacion,
			Jugador... jugadores) {
		Club club = new Club(nombre);
		club.setEntrenador(entrenador);
		club.setAsociacion(asociacion);
		for (Jugador jugador : jugadores) {
			club.addJugador(jugador);
		}
		return club;
	}
}
