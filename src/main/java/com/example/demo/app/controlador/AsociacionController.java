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

import com.example.demo.app.dto.AsociacionDTO;
import com.example.demo.app.dto.AsociacionRequest;
import com.example.demo.app.entidades.Asociacion;
import com.example.demo.app.repositorio.AsociacionRepository;

/**
 * El controlador usa el repositorio de Spring Data JPA directamente
 * (arquitectura Controlador → Repositorio → Base de datos).
 */
@RestController
@RequestMapping("/asociaciones")
public class AsociacionController {

	private final AsociacionRepository asociacionRepository;

	public AsociacionController(AsociacionRepository asociacionRepository) {
		this.asociacionRepository = asociacionRepository;
	}

	@Transactional(readOnly = true)
	@GetMapping
	public List<AsociacionDTO> listar() {
		return asociacionRepository.findAll().stream().map(AsociacionDTO::desde).toList();
	}

	@Transactional(readOnly = true)
	@GetMapping("/{id}")
	public AsociacionDTO obtener(@PathVariable Long id) {
		return AsociacionDTO.desde(buscar(id));
	}

	@Transactional
	@PostMapping
	public ResponseEntity<AsociacionDTO> crear(@RequestBody AsociacionRequest request) {
		Asociacion nueva = new Asociacion(request.nombre(), request.pais(), request.presidente());
		return ResponseEntity.status(HttpStatus.CREATED).body(AsociacionDTO.desde(asociacionRepository.save(nueva)));
	}

	@Transactional
	@PutMapping("/{id}")
	public AsociacionDTO editar(@PathVariable Long id, @RequestBody AsociacionRequest request) {
		Asociacion asociacion = buscar(id);
		asociacion.setNombre(request.nombre());
		asociacion.setPais(request.pais());
		asociacion.setPresidente(request.presidente());
		return AsociacionDTO.desde(asociacionRepository.save(asociacion));
	}

	@Transactional
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		// Si alguna asociación sigue referenciada por un club, la FK
		// clubes.id_asociacion lanza DataIntegrityViolationException (409 vía
		// ApiExceptionHandler). El flush fuerza la FK dentro del método.
		Asociacion asociacion = buscar(id);
		asociacionRepository.delete(asociacion);
		asociacionRepository.flush();
		return ResponseEntity.noContent().build();
	}

	private Asociacion buscar(Long id) {
		return asociacionRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Asociación " + id + " no existe"));
	}
}
