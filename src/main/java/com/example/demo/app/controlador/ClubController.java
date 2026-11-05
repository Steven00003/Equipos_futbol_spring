package com.example.demo.app.controlador;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.app.dto.ClubDTO;
import com.example.demo.app.dto.ClubRequest;
import com.example.demo.app.entidades.Asociacion;
import com.example.demo.app.entidades.Club;
import com.example.demo.app.entidades.Competicion;
import com.example.demo.app.entidades.Entrenador;
import com.example.demo.app.entidades.Jugador;
import com.example.demo.app.repositorio.AsociacionRepository;
import com.example.demo.app.repositorio.ClubRepository;
import com.example.demo.app.repositorio.CompeticionRepository;
import com.example.demo.app.repositorio.EntrenadorRepository;
import com.example.demo.app.repositorio.JugadorRepository;

/**
 * El controlador usa los repositorios de Spring Data JPA directamente
 * (arquitectura Controlador → Repositorio → Base de datos). Los métodos que
 * navegan relaciones LAZY armados de DTOs llevan @Transactional para que la
 * sesión de Hibernate siga abierta.
 */
@RestController
@RequestMapping("/clubes")
public class ClubController {

	private final ClubRepository clubRepository;
	private final EntrenadorRepository entrenadorRepository;
	private final AsociacionRepository asociacionRepository;
	private final CompeticionRepository competicionRepository;
	private final JugadorRepository jugadorRepository;

	public ClubController(ClubRepository clubRepository,
			EntrenadorRepository entrenadorRepository,
			AsociacionRepository asociacionRepository,
			CompeticionRepository competicionRepository,
			JugadorRepository jugadorRepository) {
		this.clubRepository = clubRepository;
		this.entrenadorRepository = entrenadorRepository;
		this.asociacionRepository = asociacionRepository;
		this.competicionRepository = competicionRepository;
		this.jugadorRepository = jugadorRepository;
	}

	@Transactional(readOnly = true)
	@GetMapping
	public List<ClubDTO> listar() {
		return clubRepository.findAll().stream().map(ClubDTO::desde).toList();
	}

	@Transactional(readOnly = true)
	@GetMapping("/{id}")
	public ClubDTO obtener(@PathVariable Long id) {
		return ClubDTO.desde(buscar(id));
	}

	@Transactional
	@PostMapping
	public ResponseEntity<ClubDTO> crear(@RequestBody ClubRequest request) {
		Club club = new Club(request.nombre());
		aplicarReferencias(club, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ClubDTO.desde(clubRepository.save(club)));
	}

	@Transactional
	@PutMapping("/{id}")
	public ClubDTO editar(@PathVariable Long id, @RequestBody ClubRequest request) {
		Club club = buscar(id);
		club.setNombre(request.nombre());
		// Si la petición trae ids nuevos, se reasignan completamente las relaciones;
		// si trae null, quedan sin asociar (null).
		club.setEntrenador(null);
		club.setAsociacion(null);
		club.getCompeticiones().clear();
		aplicarReferencias(club, request);
		return ClubDTO.desde(club);
	}

	@Transactional
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		// La relación con jugadores tiene @OnDelete(CASCADE): el motor de BD
		// borra los jugadores del club (ON DELETE CASCADE en jugadores.id_club).
		// El flush fuerza la FK en la BD dentro del método, para que una
		// violación salga aquí y no en el commit: llega al @RestControllerAdvice
		// y responde 409.
		Club club = buscar(id);
		clubRepository.delete(club);
		clubRepository.flush();
		return ResponseEntity.noContent().build();
	}

	// Agrega un jugador existente al club (FK jugadores.id_club)
	@Transactional
	@PostMapping("/{id}/jugadores/{jugadorId}")
	public ClubDTO agregarJugador(@PathVariable Long id, @PathVariable Long jugadorId) {
		Club club = buscar(id);
		Jugador jugador = jugadorRepository.findById(jugadorId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Jugador " + jugadorId + " no existe"));
		if (!club.getJugadores().contains(jugador)) {
			club.addJugador(jugador);
			clubRepository.save(club);
		}
		return ClubDTO.desde(club);
	}

	// Quita el jugador del club sin borrarlo (id_club = NULL)
	@Transactional
	@DeleteMapping("/{id}/jugadores/{jugadorId}")
	public ResponseEntity<Void> quitarJugador(@PathVariable Long id, @PathVariable Long jugadorId) {
		Club club = buscar(id);
		Jugador jugador = jugadorRepository.findById(jugadorId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Jugador " + jugadorId + " no existe"));
		club.getJugadores().remove(jugador);
		clubRepository.save(club); // Hibernate actualiza jugadores.id_club = NULL
		return ResponseEntity.noContent().build();
	}

	// Valida todas las referencias del request (404 si algún id indicado no existe)
	// y asigna las relaciones del club. Se navega LAZY dentro de esta transacción.
	private void aplicarReferencias(Club club, ClubRequest request) {
		if (request.idEntrenador() != null) {
			Entrenador entrenador = entrenadorRepository.findById(request.idEntrenador())
					.orElseThrow(() -> new RecursoNoEncontradoException(
							"Entrenador " + request.idEntrenador() + " no existe"));
			club.setEntrenador(entrenador);
		}
		if (request.idAsociacion() != null) {
			Asociacion asociacion = asociacionRepository.findById(request.idAsociacion())
					.orElseThrow(() -> new RecursoNoEncontradoException(
							"Asociación " + request.idAsociacion() + " no existe"));
			club.setAsociacion(asociacion);
		}
		if (request.idsCompeticiones() != null) {
			List<Competicion> competiciones = new ArrayList<>();
			for (Long idCompeticion : request.idsCompeticiones()) {
				competiciones.add(competicionRepository.findById(idCompeticion)
						.orElseThrow(() -> new RecursoNoEncontradoException(
								"Competición " + idCompeticion + " no existe")));
			}
			club.getCompeticiones().addAll(competiciones);
		}
	}

	private Club buscar(Long id) {
		return clubRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Club " + id + " no existe"));
	}
}
