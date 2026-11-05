/* ============================================================
   Relaciones JPA · Club de fútbol
   JavaScript puro: pestañas, tarjetas de clubes, tablas CRUD
   y modales. Sin frameworks. Comentarios en español.
   ============================================================ */
"use strict";

/* ============================================================
   1. Utilidades principales: fetch reutilizable, toast, carga
   ============================================================ */

// Referencias fijas del documento
const $cargando = document.getElementById("cargando");
const $toast = document.getElementById("toast");
let temporizadorToast = null;
let cargasPendientes = 0; // peticiones en vuelo (para el indicador de carga)

// Muestra el indicador de carga solo mientras hay peticiones en curso
function actualizarCarga(delta) {
	cargasPendientes += delta;
	$cargando.classList.toggle("oculto", cargasPendientes <= 0);
}

// Aviso flotante (toast): tipo "ok", "error" o "info"
function mostrarToast(mensaje, tipo) {
	$toast.textContent = mensaje;
	$toast.className = "toast " + (tipo || "info");
	clearTimeout(temporizadorToast);
	temporizadorToast = setTimeout(() => $toast.classList.add("oculto"), 4500);
}

/**
 * Función fetch reutilizable que maneja los errores de la API.
 * Devuelve siempre { ok, status, datos }. Si la respuesta no es 2xx muestra
 * el toast: el texto del campo "error" del JSON (404/409 de la API) o,
 * si la respuesta no trae JSON, "METODO url → código" para depurar.
 */
async function apiFetch(url, opciones = {}) {
	const metodo = opciones.metodo || "GET";
	actualizarCarga(1);
	try {
		const configuracion = { method: metodo, headers: {} };
		if (opciones.cuerpo !== undefined) {
			configuracion.headers["Content-Type"] = "application/json";
			configuracion.body = JSON.stringify(opciones.cuerpo);
		}
		let respuesta;
		try {
			respuesta = await fetch(url, configuracion);
		} catch (e) {
			// Fallo de red: la API no está accesible
			mostrarToast(metodo + " " + url + " → fallo de red (¿está corriendo en el puerto 8082?)", "error");
			return { ok: false, status: 0, datos: null };
		}
		let datos = null;
		if (respuesta.status !== 204) {
			try { datos = await respuesta.json(); } catch (e) { /* cuerpo vacío o ilegible */ }
		}
		if (!respuesta.ok) {
			// Prioridad: mensaje de error de la API; si el cuerpo no era JSON,
			// se muestra método + URL + código para facilitar la depuración
			const mensaje = (datos && datos.error) || (metodo + " " + url + " → " + respuesta.status);
			mostrarToast(mensaje, "error");
			return { ok: false, status: respuesta.status, datos };
		}
		return { ok: true, status: respuesta.status, datos };
	} finally {
		actualizarCarga(-1);
	}
}

/* ============================================================
   2. Navegación por pestañas
   ============================================================ */

const PESTANAS = ["clubes", "asociaciones", "entrenadores", "jugadores", "competiciones"];

document.querySelectorAll(".pestana").forEach((boton) => {
	boton.addEventListener("click", () => activarPestana(boton.dataset.tab));
});

function activarPestana(nombre) {
	// Resalta la pestaña activa
	document.querySelectorAll(".pestana").forEach((b) => b.classList.toggle("activa", b.dataset.tab === nombre));
	// Muestra solo el panel correspondiente
	PESTANAS.forEach((p) => {
		document.getElementById("panel-" + p).classList.toggle("visible", p === nombre);
	});
	// Carga (o refresca) los datos de la pestaña
	if (nombre === "clubes") {
		cargarClubes();
	} else {
		cargarTabla(nombre); // pasa el NOMBRE de entidad; cargarTabla resuelve la URL
	}
}

/* ============================================================
   3. Modal genérico de formulario y modal de confirmación
   ============================================================ */

const $dialogoForm = document.getElementById("dialogo-formulario");
const $tituloForm = document.getElementById("titulo-formulario");
const $camposForm = document.getElementById("campos-formulario");
const $form = document.getElementById("formulario");
let accionAlGuardar = null; // callback que se invoca al enviar el formulario

document.getElementById("btn-cancelar-form").addEventListener("click", () => $dialogoForm.close());
$form.addEventListener("submit", (evento) => {
	evento.preventDefault(); // no recargar la página
	if (accionAlGuardar) { accionAlGuardar(); }
});

/**
 * Crea un campo de formulario dentro del modal y devuelve el <input>/<select>.
 * tipo: "text" | "number" | "date" | "select" | "checkbox-list"
 */
function agregarCampo(id, etiqueta, tipo, opciones) {
	const contenedor = document.createElement("div");
	contenedor.className = "campo";
	const etiquetaEl = document.createElement("label");
	etiquetaEl.setAttribute("for", id);
	etiquetaEl.textContent = etiqueta;
	contenedor.appendChild(etiquetaEl);

	let elemento;
	if (tipo === "select") {
		elemento = document.createElement("select");
		opciones.forEach(({ value, text }) => {
			const opcion = document.createElement("option");
			opcion.value = value === null ? "" : String(value);
			opcion.textContent = text;
			elemento.appendChild(opcion);
		});
	} else if (tipo === "checkbox-list") {
		elemento = null; // las casillas se agregan aparte
		if (etiqueta) {
			const titulo = document.createElement("div");
			titulo.className = "bloque-titulo";
			titulo.textContent = etiqueta;
			contenedor.replaceChildren(titulo);
		}
		contenedor.className = "campo casillas";
		opciones.forEach(({ value, text }, indice) => {
			const fila = document.createElement("label");
			fila.className = "casilla";
			const casilla = document.createElement("input");
			casilla.type = "checkbox";
			casilla.value = String(value);
			casilla.id = id + "-" + indice;
			casilla.dataset.grupo = id;
			fila.appendChild(casilla);
			fila.appendChild(document.createTextNode(text));
			contenedor.appendChild(fila);
		});
		$camposForm.appendChild(contenedor);
		return document.querySelectorAll('input[data-grupo="' + id + '"]');
	} else {
		elemento = document.createElement("input");
		elemento.type = tipo || "text";
	}
	elemento.id = id;
	elemento.name = id;
	contenedor.appendChild(elemento);
	$camposForm.appendChild(contenedor);
	return elemento;
}

// Abre el modal con un título y construye sus campos vía "constructor"
function abrirFormulario(titulo, constructor, alGuardar) {
	$tituloForm.textContent = titulo;
	$camposForm.replaceChildren();
	accionAlGuardar = alGuardar;
	constructor(); // llena $camposForm con los inputs
	$dialogoForm.showModal();
}

/* ----- Modal de confirmación (para eliminar) ----- */
const $dialogoConfirmar = document.getElementById("dialogo-confirmar");
const $textoConfirmar = document.getElementById("texto-confirmar");
let accionAlConfirmar = null;

document.getElementById("btn-confirmar-cancelar").addEventListener("click", () => $dialogoConfirmar.close());
document.getElementById("btn-confirmar-eliminar").addEventListener("click", () => {
	$dialogoConfirmar.close();
	if (accionAlConfirmar) { accionAlConfirmar(); }
});

// Pide confirmación y ejecuta la acción solo si el usuario acepta
function confirmarEliminar(mensaje, accion) {
	$textoConfirmar.textContent = mensaje;
	accionAlConfirmar = accion;
	$dialogoConfirmar.showModal();
}

/* ============================================================
   4. Configuración de las cuatro tablas simples
   Cada entidad define sus columnas y los campos del formulario.
   ============================================================ */

const NOMBRE_DE_URL = {
	asociaciones: "/asociaciones",
	entrenadores: "/entrenadores",
	jugadores: "/jugadores",
	competiciones: "/competiciones",
};

const CONFIG_TABLAS = {
	asociaciones: {
		idTabla: "tbody-asociaciones",
		sustantivo: "asociación",
		celdas: (a) => [a.id, a.nombre, a.pais, a.presidente],
		campos: [
			{ nombre: "nombre", etiqueta: "Nombre", tipo: "text" },
			{ nombre: "pais", etiqueta: "País", tipo: "text" },
			{ nombre: "presidente", etiqueta: "Presidente", tipo: "text" },
		],
		cuerpo: (valores) => ({ nombre: valores.nombre, pais: valores.pais, presidente: valores.presidente }),
	},
	entrenadores: {
		idTabla: "tbody-entrenadores",
		sustantivo: "entrenador",
		celdas: (e) => [e.id, e.nombre, e.apellido, e.edad, e.nacionalidad],
		campos: [
			{ nombre: "nombre", etiqueta: "Nombre", tipo: "text" },
			{ nombre: "apellido", etiqueta: "Apellido", tipo: "text" },
			{ nombre: "edad", etiqueta: "Edad", tipo: "number" },
			{ nombre: "nacionalidad", etiqueta: "Nacionalidad", tipo: "text" },
		],
		cuerpo: (valores) => ({
			nombre: valores.nombre, apellido: valores.apellido,
			edad: Number(valores.edad) || 0, nacionalidad: valores.nacionalidad,
		}),
	},
	jugadores: {
		idTabla: "tbody-jugadores",
		sustantivo: "jugador",
		celdas: (j) => [j.id, j.numero, j.nombre, j.apellido, j.posicion],
		campos: [
			{ nombre: "nombre", etiqueta: "Nombre", tipo: "text" },
			{ nombre: "apellido", etiqueta: "Apellido", tipo: "text" },
			{ nombre: "numero", etiqueta: "Número", tipo: "number" },
			{ nombre: "posicion", etiqueta: "Posición", tipo: "text" },
		],
		cuerpo: (valores) => ({
			nombre: valores.nombre, apellido: valores.apellido,
			numero: Number(valores.numero) || 0, posicion: valores.posicion,
		}),
	},
	competiciones: {
		idTabla: "tbody-competiciones",
		sustantivo: "competición",
		celdas: (c) => [c.id, c.nombre, c.montoPremio, c.fechaInicio, c.fechaFin],
		campos: [
			{ nombre: "nombre", etiqueta: "Nombre", tipo: "text" },
			{ nombre: "montoPremio", etiqueta: "Monto del premio", tipo: "number" },
			{ nombre: "fechaInicio", etiqueta: "Fecha de inicio", tipo: "date" },
			{ nombre: "fechaFin", etiqueta: "Fecha de fin", tipo: "date" },
		],
		cuerpo: (valores) => ({
			nombre: valores.nombre, montoPremio: Number(valores.montoPremio) || 0,
			fechaInicio: valores.fechaInicio || null, fechaFin: valores.fechaFin || null,
		}),
	},
};

/* ============================================================
   5. CRUD genérico para las cuatro tablas simples
   ============================================================ */

// Botones "+ Nuevo ..." de cada tabla
document.querySelectorAll("[data-nuevo]").forEach((boton) => {
	boton.addEventListener("click", () => abrirFormularioTabla(boton.dataset.nuevo, null));
});

// Carga la tabla de una entidad y alterna el estado vacío
async function cargarTabla(entidad) {
	const respuesta = await apiFetch(NOMBRE_DE_URL[entidad]);
	if (!respuesta.ok) { return; }
	const registros = respuesta.datos || [];
	const cuerpo = document.getElementById(CONFIG_TABLAS[entidad].idTabla);
	cuerpo.replaceChildren();
	registros.forEach((registro) => cuerpo.appendChild(filaTabla(entidad, registro)));
	document.getElementById("vacio-" + entidad).classList.toggle("oculto", registros.length > 0);
}

// Construye una fila con celdas + botones Editar y Eliminar (sin innerHTML)
function filaTabla(entidad, registro) {
	const config = CONFIG_TABLAS[entidad];
	const fila = document.createElement("tr");
	config.celdas(registro).forEach((texto) => {
		const celda = document.createElement("td");
		celda.textContent = texto === null || texto === undefined ? "—" : String(texto);
		fila.appendChild(celda);
	});
	const celdaAcciones = document.createElement("td");
	celdaAcciones.className = "col-acciones";
	const acciones = document.createElement("div");
	acciones.className = "acciones-tabla";
	const btnEditar = document.createElement("button");
	btnEditar.type = "button";
	btnEditar.className = "boton mini";
	btnEditar.textContent = "Editar";
	btnEditar.addEventListener("click", () => abrirFormularioTabla(entidad, registro));
	const btnEliminar = document.createElement("button");
	btnEliminar.type = "button";
	btnEliminar.className = "boton mini peligro";
	btnEliminar.textContent = "Eliminar";
	btnEliminar.addEventListener("click", () => confirmarEliminar(
		"¿Eliminar la " + config.sustantivo + " " + registro.id + "?", () => eliminarRegistro(entidad, registro.id)));
	acciones.append(btnEditar, btnEliminar);
	celdaAcciones.appendChild(acciones);
	fila.appendChild(celdaAcciones);
	return fila;
}

// Formulario de crear/editar para entidades con tabla
function abrirFormularioTabla(entidad, registro) {
	const config = CONFIG_TABLAS[entidad];
	const url = NOMBRE_DE_URL[entidad];
	abrirFormulario(
		registro ? "Editar " + config.sustantivo : "Nueva " + config.sustantivo,
		() => {
			// Genera los campos que necesita el Request DTO de esta entidad
			config.campos.forEach((campo) => {
				const control = agregarCampo("f-" + campo.nombre, campo.etiqueta, campo.tipo);
				if (registro) { control.value = registro[campo.nombre] ?? ""; }
			});
		},
		async () => {
			// Recoge los valores de los inputs generados y arma el Request DTO
			const valores = {};
			config.campos.forEach((campo) => {
				valores[campo.nombre] = document.getElementById("f-" + campo.nombre).value;
			});
			const cuerpo = config.cuerpo(valores);
			const respuesta = registro
				? await apiFetch(url + "/" + registro.id, { metodo: "PUT", cuerpo })
				: await apiFetch(url, { metodo: "POST", cuerpo });
			if (respuesta.ok) {
				$dialogoForm.close();
				mostrarToast(registro ? "Actualizado" : "Creado", "ok");
				cargarTabla(entidad);
			}
			// Si falló (404/409...), el toast ya lo mostró apiFetch y el modal queda abierto
		}
	);
}

// Elimina un registro tras la confirmación del modal
async function eliminarRegistro(entidad, id) {
	const respuesta = await apiFetch(NOMBRE_DE_URL[entidad] + "/" + id, { metodo: "DELETE" });
	if (respuesta.ok) {
		mostrarToast("Eliminado", "ok");
		cargarTabla(entidad);
	}
}

/* ============================================================
   6. Clubes: tarjetas con las cuatro relaciones JPA
   ============================================================ */

const $contenedorClubes = document.getElementById("contenedor-clubes");
const $vacioClubes = document.getElementById("vacio-clubes");

document.getElementById("btn-nuevo-club").addEventListener("click", () => abrirFormularioClub(null));

// Descarga todos los clubes y pinta una tarjeta por club
async function cargarClubes() {
	const respuesta = await apiFetch("/clubes");
	if (!respuesta.ok) { return; }
	const clubes = respuesta.datos || [];
	$contenedorClubes.replaceChildren();
	clubes.forEach((club) => $contenedorClubes.appendChild(tarjetaClub(club)));
	$vacioClubes.classList.toggle("oculto", clubes.length > 0);
}

// Crea la tarjeta de un club usando solo createElement/textContent (sin innerHTML)
function tarjetaClub(club) {
	const tarjeta = document.createElement("article");
	tarjeta.className = "tarjeta";

	// Cabecera: nombre del club
	const cabecera = document.createElement("div");
	cabecera.className = "tarjeta-cabecera";
	const nombre = document.createElement("h3");
	nombre.textContent = club.nombre || "(sin nombre)";
	cabecera.append(nombre);

	// ---- Bloque Entrenador · OneToOne ----
	const bloqueEntrenador = document.createElement("div");
	bloqueEntrenador.className = "bloque";
	bloqueEntrenador.appendChild(tituloBloque("Entrenador", "OneToOne"));
	const valorEntrenador = document.createElement("p");
	valorEntrenador.className = "valor";
	valorEntrenador.textContent = club.entrenador
		? club.entrenador.nombre + " " + club.entrenador.apellido + " (" + club.entrenador.nacionalidad + ", " + club.entrenador.edad + " años)"
		: "Sin entrenador";
	if (!club.entrenador) { valorEntrenador.classList.add("suave"); }
	bloqueEntrenador.appendChild(valorEntrenador);

	// ---- Bloque Asociación · ManyToOne ----
	const bloqueAsociacion = document.createElement("div");
	bloqueAsociacion.className = "bloque";
	bloqueAsociacion.appendChild(tituloBloque("Asociación", "ManyToOne"));
	const valorAsociacion = document.createElement("p");
	valorAsociacion.className = "valor";
	valorAsociacion.textContent = club.asociacion ? club.asociacion.nombre + " · " + club.asociacion.pais : "Sin asociación";
	if (!club.asociacion) { valorAsociacion.classList.add("suave"); }
	bloqueAsociacion.appendChild(valorAsociacion);

	// ---- Bloque Plantilla · OneToMany ----
	const bloquePlantilla = document.createElement("div");
	bloquePlantilla.className = "bloque";
	bloquePlantilla.appendChild(tituloBloque("Plantilla", "OneToMany"));
	const lista = document.createElement("ul");
	lista.className = "lista-jugadores";
	if (club.jugadores && club.jugadores.length > 0) {
		club.jugadores.forEach((jugador) => {
			const item = document.createElement("li");
			const datos = document.createElement("span");
			datos.className = "datos";
			datos.textContent = "#" + jugador.numero + " " + jugador.nombre + " " + jugador.apellido + " — " + jugador.posicion;
			const btnQuitar = document.createElement("button");
			btnQuitar.type = "button";
			btnQuitar.className = "boton mini";
			btnQuitar.textContent = "Quitar";
			btnQuitar.title = "Quitar del club (no lo borra)";
			btnQuitar.addEventListener("click", () => quitarJugadorDelClub(club.id, jugador));
			item.append(datos, btnQuitar);
			lista.appendChild(item);
		});
	} else {
		const item = document.createElement("li");
		const datos = document.createElement("span");
		datos.className = "datos suave";
		datos.textContent = "Sin jugadores";
		item.appendChild(datos);
		lista.appendChild(item);
	}
	bloquePlantilla.appendChild(lista);

	// ---- Bloque Competiciones · ManyToMany (etiquetas) ----
	const bloqueCompeticiones = document.createElement("div");
	bloqueCompeticiones.className = "bloque";
	bloqueCompeticiones.appendChild(tituloBloque("Competiciones", "ManyToMany"));
	const chips = document.createElement("div");
	chips.className = "chips";
	if (club.competiciones && club.competiciones.length > 0) {
		club.competiciones.forEach((competicion) => {
			const chip = document.createElement("span");
			chip.className = "chip";
			chip.textContent = competicion.nombre;
			chips.appendChild(chip);
		});
	} else {
		const chip = document.createElement("span");
		chip.className = "chip";
		chip.textContent = "Sin competiciones";
		chips.appendChild(chip);
	}
	bloqueCompeticiones.appendChild(chips);

	// ---- Pie: Agregar jugador / Editar / Eliminar ----
	const pie = document.createElement("div");
	pie.className = "tarjeta-pie";
	const btnAgregarJugador = document.createElement("button");
	btnAgregarJugador.type = "button";
	btnAgregarJugador.className = "boton mini";
	btnAgregarJugador.textContent = "+ Agregar jugador";
	btnAgregarJugador.addEventListener("click", () => abrirAgregarJugador(club));
	const acciones = document.createElement("div");
	acciones.className = "acciones";
	const btnEditar = document.createElement("button");
	btnEditar.type = "button";
	btnEditar.className = "boton mini";
	btnEditar.textContent = "Editar";
	btnEditar.addEventListener("click", () => abrirFormularioClub(club));
	const btnEliminar = document.createElement("button");
	btnEliminar.type = "button";
	btnEliminar.className = "boton mini peligro";
	btnEliminar.textContent = "Eliminar";
	btnEliminar.addEventListener("click", () => confirmarEliminar(
		"¿Eliminar el club «" + club.nombre + "»? Sus jugadores también se borran (ON DELETE CASCADE).",
		() => eliminarClub(club)));
	acciones.append(btnEditar, btnEliminar);
	pie.append(btnAgregarJugador, acciones);

	tarjeta.append(cabecera, bloqueEntrenador, bloqueAsociacion, bloquePlantilla, bloqueCompeticiones, pie);
	return tarjeta;
}

// Título de bloque con la etiqueta discreta de la relación JPA
function tituloBloque(texto, relacion) {
	const fila = document.createElement("div");
	fila.className = "bloque-titulo";
	const nombre = document.createElement("span");
	nombre.textContent = texto;
	const etiqueta = document.createElement("span");
	etiqueta.className = "relacion";
	etiqueta.textContent = relacion;
	fila.append(nombre, etiqueta);
	return fila;
}

/* ----- Formulario de club (modal): nombre, entrenador, asociación, competiciones ----- */
async function abrirFormularioClub(club) {
	// Los selectores se llenan con los listados de la API
	const [rEntrenadores, rAsociaciones, rCompeticiones] = await Promise.all([
		apiFetch("/entrenadores"), apiFetch("/asociaciones"), apiFetch("/competiciones"),
	]);
	if (!rEntrenadores.ok || !rAsociaciones.ok || !rCompeticiones.ok) { return; }

	const idsEnClub = new Set((club && club.jugadores ? club.jugadores : []).map((j) => j.id));
	const opcionesEntrenador = [{ value: null, text: "— Sin entrenador —" }]
		.concat(rEntrenadores.datos.map((e) => ({ value: e.id, text: e.nombre + " " + e.apellido + " (#" + e.id + ")" })));
	const opcionesAsociacion = [{ value: null, text: "— Sin asociación —" }]
		.concat(rAsociaciones.datos.map((a) => ({ value: a.id, text: a.nombre + " (" + a.pais + ")" })));
	const opcionesCompeticiones = rCompeticiones.datos.map((c) => ({ value: c.id, text: c.nombre }));

	abrirFormulario(club ? "Editar club" : "Nuevo club", () => {
		agregarCampo("c-nombre", "Nombre del club", "text").value = club ? (club.nombre || "") : "";
		const seleccionEntrenador = agregarCampo("c-entrenador", "Entrenador (OneToOne)", "select", opcionesEntrenador);
		const seleccionAsociacion = agregarCampo("c-asociacion", "Asociación (ManyToOne)", "select", opcionesAsociacion);
		// Casillas de competiciones (ManyToMany): la etiqueta es del grupo
		agregarCampo("c-competiciones", "Competiciones (ManyToMany)", "checkbox-list", opcionesCompeticiones);
		if (club) {
			if (club.entrenador) { seleccionEntrenador.value = String(club.entrenador.id); }
			if (club.asociacion) { seleccionAsociacion.value = String(club.asociacion.id); }
			// Marca las casillas de las competiciones que ya juega el club
			(club.competiciones || []).forEach((c) => {
				const casilla = document.querySelector('input[data-grupo="c-competiciones"][value="' + c.id + '"]');
				if (casilla) { casilla.checked = true; }
			});
		}
	}, async () => {
		// Arma el ClubRequest: { nombre, idEntrenador, idAsociacion, idsCompeticiones }
		const idEntrenador = document.getElementById("c-entrenador").value;
		const idAsociacion = document.getElementById("c-asociacion").value;
		const idsCompeticiones = Array.from(document.querySelectorAll('input[data-grupo="c-competiciones"]:checked'))
			.map((casilla) => Number(casilla.value));
		const cuerpo = {
			nombre: document.getElementById("c-nombre").value || null,
			idEntrenador: idEntrenador === "" ? null : Number(idEntrenador),
			idAsociacion: idAsociacion === "" ? null : Number(idAsociacion),
			idsCompeticiones,
		};
		const respuesta = club
			? await apiFetch("/clubes/" + club.id, { metodo: "PUT", cuerpo })
			: await apiFetch("/clubes", { metodo: "POST", cuerpo });
		if (respuesta.ok) {
			$dialogoForm.close();
			mostrarToast(club ? "Club actualizado" : "Club creado", "ok");
			cargarClubes();
		}
	});
}

/* ----- Diálogo para agregar un jugador existente al club ----- */
async function abrirAgregarJugador(club) {
	const respuesta = await apiFetch("/jugadores");
	if (!respuesta.ok) { return; }
	const idsEnClub = new Set((club.jugadores || []).map((j) => j.id));
	// Solo se listan jugadores que aún no pertenecen al club
	const disponibles = (respuesta.datos || []).filter((j) => !idsEnClub.has(j.id));
	if (disponibles.length === 0) {
		mostrarToast("No hay jugadores disponibles sin club.", "info");
		return;
	}
	const opciones = disponibles.map((j) => ({
		value: j.id,
		text: "#" + j.numero + " " + j.nombre + " " + j.apellido + " — " + j.posicion,
	}));
	abrirFormulario("Agregar jugador a «" + club.nombre + "»", () => {
		agregarCampo("a-jugador", "Jugador", "select", opciones).value = String(opciones[0].value);
	}, async () => {
		const jugadorId = document.getElementById("a-jugador").value;
		if (jugadorId === "") { return; }
		// POST /clubes/{id}/jugadores/{jugadorId}
		const resultado = await apiFetch("/clubes/" + club.id + "/jugadores/" + jugadorId, { metodo: "POST" });
		if (resultado.ok) {
			$dialogoForm.close();
			mostrarToast("Jugador agregado al club", "ok");
			cargarClubes();
		}
	});
}

/* ----- Quita un jugador del club (DELETE /clubes/{id}/jugadores/{jugadorId}) ----- */
async function quitarJugadorDelClub(idClub, jugador) {
	const respuesta = await apiFetch("/clubes/" + idClub + "/jugadores/" + jugador.id, { metodo: "DELETE" });
	if (respuesta.ok) {
		mostrarToast(jugador.nombre + " " + jugador.apellido + " salió del club", "ok");
		cargarClubes();
	}
}

/* ----- Elimina un club tras confirmación (el motor de BD borra sus jugadores) ----- */
async function eliminarClub(club) {
	const respuesta = await apiFetch("/clubes/" + club.id, { metodo: "DELETE" });
	if (respuesta.ok) {
		mostrarToast("Club «" + club.nombre + "» eliminado", "ok");
		cargarClubes();
	}
}

/* ============================================================
   7. Arranque: carga la primera pestaña
   ============================================================ */
cargarClubes();
