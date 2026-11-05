package com.example.demo.app.controlador;

/**
 * Se lanza cuando no existe el recurso o una referencia (idEntrenador,
 * idAsociacion, etc.). La captura ApiExceptionHandler y responde 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

	public RecursoNoEncontradoException(String mensaje) {
		super(mensaje);
	}
}
