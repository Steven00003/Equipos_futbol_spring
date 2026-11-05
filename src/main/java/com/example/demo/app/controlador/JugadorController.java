package com.example.demo.app.controlador;

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

import com.example.demo.app.dto.JugadorDTO;
import com.example.demo.app.dto.JugadorRequest;
import com.example.demo.app.entidades.Jugador;
import com.example.demo.app.repositorio.JugadorRepository;

/**
 * El controlador usa el repositorio de Spring Data JPA directamente
 * (arquitectura Controlador → Repositorio → Base de datos).
 */
@RestController
@RequestMapping("/jugadores")
public class JugadorController {

	private final JugadorRepository jugadorRepository;

	public JugadorController(JugadorRepository jugadorRepository) {
		this.jugadorRepository = jugadorRepository;
	}

	@Transactional(readOnly = true)
	@GetMapping
	public List<JugadorDTO> listar() {
		return jugadorRepository.findAll().stream().map(JugadorDTO::desde).toList();
	}

	@Transactional(readOnly = true)
	@GetMapping("/{id}")
	public JugadorDTO obtener(@PathVariable Long id) {
		return JugadorDTO.desde(buscar(id));
	}

	@Transactional
	@PostMapping
	public ResponseEntity<JugadorDTO> crear(@RequestBody JugadorRequest request) {
		Jugador nuevo = new Jugador(request.nombre(), request.apellido(),
				request.numero(), request.posicion());
		// Se crea sin club; POST /clubes/{id}/jugadores/{jugadorId} lo asigna después.
		return ResponseEntity.status(HttpStatus.CREATED).body(JugadorDTO.desde(jugadorRepository.save(nuevo)));
	}

	@Transactional
	@PutMapping("/{id}")
	public JugadorDTO editar(@PathVariable Long id, @RequestBody JugadorRequest request) {
		Jugador jugador = buscar(id);
		jugador.setNombre(request.nombre());
		jugador.setApellido(request.apellido());
		jugador.setNumero(request.numero());
		jugador.setPosicion(request.posicion());
		return JugadorDTO.desde(jugadorRepository.save(jugador));
	}

	@Transactional
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		// Si el jugador pertenece a un club, la FK jugadores.id_club lanza
		// DataIntegrityViolationException (409 vía ApiExceptionHandler).
		// El flush fuerza la FK dentro del método, no en el commit.
		Jugador jugador = buscar(id);
		jugadorRepository.delete(jugador);
		jugadorRepository.flush();
		return ResponseEntity.noContent().build();
	}

	private Jugador buscar(Long id) {
		return jugadorRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Jugador " + id + " no existe"));
	}
}
