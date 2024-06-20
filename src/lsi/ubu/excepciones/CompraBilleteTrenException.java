/*
 * Nombre:CompraBilleteTrenException
 * Descripción: Clase de la excepciones que usaremos en la practica
 * Autores:Álvaro Villar, David Ibeas y Aitor Blanco
 * Ver:0.1.0
 */
package lsi.ubu.excepciones;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CompraBilleteTrenException: Implementa las excepciones contextualizadas de la
 * transaccion de CompraBilleteTren
 * 
 * @author <a href="mailto:jmaudes@ubu.es">Jes�s Maudes</a>
 * @author <a href="mailto:rmartico@ubu.es">Ra�l Marticorena</a>
 * @version 1.0
 * @since 1.0
 */
public class CompraBilleteTrenException extends SQLException {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(CompraBilleteTrenException.class);

	public static final int NO_PLAZAS = 1; //En caso de que no haya suficientes plazas para comprar tendra que recibir 1
	public static final int NO_EXISTE_VIAJE = 2; //En caso de que no exista el viaje tendra que recibir 2
	public static final int NO_RESERVAS = 3;//En caso de que no se hayan realizado reservas
	public static final int NO_TICKET = 4; //En caso de que el ticket seleccionado no se encuentre

	private int codigo; 
	private String mensaje;

	public CompraBilleteTrenException(int code) {

		codigo=code;//Guardamos el codigo del error que hemos llamado
		if(codigo==NO_PLAZAS) { //en caso de que sea igual al codigo de error de que no hay suficientes plazas
			mensaje="El viaje que has escogido no tiene suficiente plazas"; //Guardamos el mensaje de error apropiado
		}else if(codigo==NO_EXISTE_VIAJE) {//en caso de que sea igual al codifo de error de que no existe el viaje
			mensaje="No existe el viaje seleccionado";//Guardamos el mensaje de error apropiado
		}else if(codigo==NO_RESERVAS) {
			mensaje="El número de plazas a anular es mayor que las plazas reservadas en el ticket";
		}else if(codigo==NO_TICKET) {
			mensaje="El ticket seleccionado no se ha encontrado";
		}
		

		LOGGER.debug(mensaje);//Guardamos el mensaje en el logger

		// Traza_de_pila
		for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			LOGGER.debug(ste.toString());
		}
	}

	@Override
	public String getMessage() { // Redefinicion del metodo de la clase Exception
		return mensaje;
	}

	@Override
	public int getErrorCode() { // Redefinicion del metodo de la clase SQLException
		return codigo;
	}
}

