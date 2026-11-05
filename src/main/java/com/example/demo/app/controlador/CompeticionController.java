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

import com.example.demo.app.dto.CompeticionDTO;
import com.example.demo.app.dto.CompeticionRequest;
import com.example.demo.app.entidades.Competicion;
import com.example.demo.app.repositorio.CompeticionRepository;

/**
 * El controlador usa el repositorio de Spring Data JPA directamente
 * (arquitectura Controlador → Repositorio → Base de datos).
 */
@RestController
@RequestMapping("/competiciones")
public class CompeticionController {

	private final CompeticionRepository competicionRepository;

	public CompeticionController(CompeticionRepository competicionRepository) {
		this.competicionRepository = competicionRepository;
	}

	@Transactional(readOnly = true)
	@GetMapping
	public List<CompeticionDTO> listar() {
		return competicionRepository.findAll().stream().map(CompeticionDTO::desde).toList();
	}

	@Transactional(readOnly = true)
	@GetMapping("/{id}")
	public CompeticionDTO obtener(@PathVariable Long id) {
		return CompeticionDTO.desde(buscar(id));
	}

	@Transactional
	@PostMapping
	public ResponseEntity<CompeticionDTO> crear(@RequestBody CompeticionRequest request) {
		Competicion nueva = new Competicion(request.nombre(), request.montoPremio(),
				request.fechaInicio(), request.fechaFin());
		return ResponseEntity.status(HttpStatus.CREATED).body(CompeticionDTO.desde(competicionRepository.save(nueva)));
	}

	@Transactional
	@PutMapping("/{id}")
	public CompeticionDTO editar(@PathVariable Long id, @RequestBody CompeticionRequest request) {
		Competicion competicion = buscar(id);
		competicion.setNombre(request.nombre());
		competicion.setMontoPremio(request.montoPremio());
		competicion.setFechaInicio(request.fechaInicio());
		competicion.setFechaFin(request.fechaFin());
		return CompeticionDTO.desde(competicionRepository.save(competicion));
	}

	@Transactional
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		// Si la competición sigue inscrita en algún club, la FK de
		// clubes_competiciones lanza DataIntegrityViolationException (409 vía
		// ApiExceptionHandler). El flush fuerza la FK dentro del método.
		Competicion competicion = buscar(id);
		competicionRepository.delete(competicion);
		competicionRepository.flush();
		return ResponseEntity.noContent().build();
	}

	private Competicion buscar(Long id) {
		return competicionRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Competición " + id + " no existe"));
	}
}
