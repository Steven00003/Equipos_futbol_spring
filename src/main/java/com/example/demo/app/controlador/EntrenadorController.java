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

import com.example.demo.app.dto.EntrenadorDTO;
import com.example.demo.app.dto.EntrenadorRequest;
import com.example.demo.app.entidades.Entrenador;
import com.example.demo.app.repositorio.EntrenadorRepository;

/**
 * El controlador usa el repositorio de Spring Data JPA directamente
 * (arquitectura Controlador → Repositorio → Base de datos).
 */
@RestController
@RequestMapping("/entrenadores")
public class EntrenadorController {

	private final EntrenadorRepository entrenadorRepository;

	public EntrenadorController(EntrenadorRepository entrenadorRepository) {
		this.entrenadorRepository = entrenadorRepository;
	}

	@Transactional(readOnly = true)
	@GetMapping
	public List<EntrenadorDTO> listar() {
		return entrenadorRepository.findAll().stream().map(EntrenadorDTO::desde).toList();
	}

	@Transactional(readOnly = true)
	@GetMapping("/{id}")
	public EntrenadorDTO obtener(@PathVariable Long id) {
		return EntrenadorDTO.desde(buscar(id));
	}

	@Transactional
	@PostMapping
	public ResponseEntity<EntrenadorDTO> crear(@RequestBody EntrenadorRequest request) {
		Entrenador nuevo = new Entrenador(request.nombre(), request.apellido(),
				request.edad(), request.nacionalidad());
		return ResponseEntity.status(HttpStatus.CREATED).body(EntrenadorDTO.desde(entrenadorRepository.save(nuevo)));
	}

	@Transactional
	@PutMapping("/{id}")
	public EntrenadorDTO editar(@PathVariable Long id, @RequestBody EntrenadorRequest request) {
		Entrenador entrenador = buscar(id);
		entrenador.setNombre(request.nombre());
		entrenador.setApellido(request.apellido());
		entrenador.setEdad(request.edad());
		entrenador.setNacionalidad(request.nacionalidad());
		return EntrenadorDTO.desde(entrenadorRepository.save(entrenador));
	}

	@Transactional
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		// El flush fuerza la FK en la BD dentro del método: si hay violación
		// (el entrenador está asignado a un club), el error llega al
		// @RestControllerAdvice y responde 409.
		Entrenador entrenador = buscar(id);
		entrenadorRepository.delete(entrenador);
		entrenadorRepository.flush();
		return ResponseEntity.noContent().build();
	}

	private Entrenador buscar(Long id) {
		return entrenadorRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Entrenador " + id + " no existe"));
	}
}
