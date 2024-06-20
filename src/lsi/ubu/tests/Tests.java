/*
 * Nombre: Tests
 * Descripción: Clase para poder comprobar el correcto funcionamiento de compras
 * y anulaciones de billetes de tren.
 * Autores: Álvaro Villar, David Ibeas y Aitor Blanco.
 */

package lsi.ubu.tests;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lsi.ubu.excepciones.CompraBilleteTrenException;
import lsi.ubu.servicios.Servicio;
import lsi.ubu.servicios.ServicioImpl;
import lsi.ubu.util.PoolDeConexiones;

//Clase que implementa los tests para las compras y anulaciones de billetes de tren.
public class Tests {

	/** Logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(Tests.class);

	public static final String ORIGEN = "Burgos";
	public static final String DESTINO = "Madrid";

	// Tests asociados a las anulaciones de los billetes de tren.
	public void ejecutarTestsAnularBilletes() {

		Servicio servicio = new ServicioImpl();
		PoolDeConexiones pool = PoolDeConexiones.getInstance();
		Connection con = null;
		PreparedStatement st = null;
		ResultSet rs = null;

		// Prueba el caso de que intentamos anular un billete de un viaje que no existe.
		try {
			java.util.Date fecha = toDate("15/04/2010");
			Time hora = Time.valueOf("12:00:00");
			String origen = "Origen";
			String destino = "Destino";
			int nroPlazas = 2;
			int idTicket = 2;

			// Intentamos anular un billete de un viaje que no existe.
			servicio.anularBillete(hora, fecha, origen, destino, nroPlazas, idTicket);

			// Si llegamos aquí, significa que ha anulado un billete de un viaje que no
			// existe.
			LOGGER.info("NO se da cuenta de que no existe el viaje asociado al billete.");
		} catch (SQLException e) {
			// Si llegamos aquí, significa que ha detectado que el viaje del billete no
			// existe.
			if (e.getErrorCode() == CompraBilleteTrenException.NO_EXISTE_VIAJE) {
			    LOGGER.info("Se da cuenta de que no existe el viaje asociado al billete");
			}
		}

		// Prueba el caso de que intentamos anular un billete que no existe.
		try {
			java.util.Date fecha = toDate("20/04/2022");
			Time hora = Time.valueOf("8:30:00");
			int nroPlazas = 2;
			int idTicket = 999;

			// Intentamos anular un billete que no existe.
			servicio.anularBillete(hora, fecha, ORIGEN, DESTINO, nroPlazas, idTicket);

			// Si llegamos aquí, significa que ha anulado un billete que no existe.
			LOGGER.info("NO se da cuenta de que no existe el billete.");
		} catch (SQLException e) {
			// Si llegamos aquí, significa que ha detectado un billete que no existe.
			if (e.getErrorCode() == CompraBilleteTrenException.NO_TICKET) {
			    LOGGER.info("Se se da cuenta de que no existe el billete.");
            }
		}

		// Prueba el caso de que intentamos anular mas plazas de las que se reservaron.
		try {
			java.util.Date fecha = toDate("20/04/2022");
			Time hora = Time.valueOf("8:30:00");
			int nroPlazas = 3;
			int idTicket = 2;

			// Itentamos anular mas plazas de las que se reservaron.
			servicio.anularBillete(hora, fecha, ORIGEN, DESTINO, nroPlazas, idTicket);

			// Si llegamos aquí, significa que ha anulado mas plazas de las que se
			// reservaron.
			LOGGER.info("NO se da cuenta de que hay mas plazas de las que se reservaron.");
		} catch (SQLException e) {
			// Si llegamos aquí, significa que ha detectado mas plazas de las que se
			// reservaron.
			if (e.getErrorCode() == CompraBilleteTrenException.NO_RESERVAS) {
			    LOGGER.info("Se da cuenta de que hay mas plazas de las que se reservaron.");
			}
		}

		// Prueba el caso de un anulacción de un ticket correcto.
		try {
			java.util.Date fecha = toDate("20/04/2022");
			Time hora = Time.valueOf("8:30:00");
			int nroPlazas = 2;
			int idTicket = 2;

			// Intentamos anular el billete.
			servicio.anularBillete(hora, fecha, ORIGEN, DESTINO, nroPlazas, idTicket);

			// Comprobamos que el billete se haya anulado correctamente.
			con = pool.getConnection();
			st = con.prepareStatement("SELECT * FROM tickets WHERE idTicket = ?");

			st.setInt(1, idTicket);
			rs = st.executeQuery();

			if (!rs.next()) {
				// Si llegamos aquí, significa que ha anulado un billete que existe.
				LOGGER.info("Anula billete OK");
			} else {
				// Si llegamos aquí, significa que no ha anulado un billete que no existe.
				LOGGER.info("Anula billete MAL");
			}
		} catch (SQLException e) {
			// Si llegamos aquí, significa que ha surgido un error inesperado.
			LOGGER.info("Error inesperado MAL");
		}
	}

	// Tests asociados a la compra de billetes de tren.
	public void ejecutarTestsCompraBilletes() {

		Servicio servicio = new ServicioImpl();
		PoolDeConexiones pool = PoolDeConexiones.getInstance();
		Connection con = null;
		PreparedStatement st = null;
		ResultSet rs = null;

		// Prueba el caso de que no existe el viaje.
		try {

			java.util.Date fecha = toDate("15/04/2010");
			Time hora = Time.valueOf("12:00:00");
			int nroPlazas = 3;

			servicio.comprarBillete(hora, fecha, ORIGEN, DESTINO, nroPlazas);

			LOGGER.info("NO se da cuenta de que no existe el viaje MAL");

		} catch (SQLException e) {

			if (e.getErrorCode() == CompraBilleteTrenException.NO_EXISTE_VIAJE) {
				LOGGER.info("Se da cuenta de que no existe el viaje OK");
			}
		}

		// Prueba el caso de que exista el viaje pero no haya plazas.
		try {

			java.util.Date fecha = toDate("20/04/2022");
			Time hora = Time.valueOf("8:30:00");
			int nroPlazas = 50;

			servicio.comprarBillete(hora, fecha, ORIGEN, DESTINO, nroPlazas);

			LOGGER.info("NO se da cuenta de que no hay plazas MAL");

		} catch (SQLException e) {

			if (e.getErrorCode() == CompraBilleteTrenException.NO_PLAZAS) {
				LOGGER.info("Se da cuenta de que no hay plazas OK");
			}

		}

		// Prueba el caso de que exista el viaje y si hay plazas.
		try {

			java.util.Date fecha = toDate("20/04/2022");
			Time hora = Time.valueOf("8:30:00");
			int nroPlazas = 5;

			servicio.comprarBillete(hora, fecha, ORIGEN, DESTINO, nroPlazas);

			con = pool.getConnection();
			st = con.prepareStatement(
					" SELECT IDVIAJE||IDTREN||IDRECORRIDO||FECHA||NPLAZASLIBRES||REALIZADO||IDCONDUCTOR||IDTICKET||CANTIDAD||PRECIO "
							+ " FROM VIAJES natural join tickets "
							+ " where idticket=3 and trunc(fechacompra) = trunc(current_date) ");
			rs = st.executeQuery();

			String resultadoReal = "";
			while (rs.next()) {
				resultadoReal += rs.getString(1);
			}

			String resultadoEsperado = "11120/04/2225113550";
			// LOGGER.info("R"+resultadoReal);
			// LOGGER.info("E"+resultadoEsperado);
			if (resultadoReal.equals(resultadoEsperado)) {
				LOGGER.info("Compra ticket OK");
			} else {
				LOGGER.info("Compra ticket MAL");
			}

		} catch (SQLException e) {
			LOGGER.info("Error inesperado MAL");
		}
	}

	private java.util.Date toDate(String miString) { // convierte una cadena en fecha
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy"); // Las M en mayusculas porque sino interpreta
										// minutos!!
			java.util.Date fecha = sdf.parse(miString);
			return fecha;
		} catch (ParseException e) {
			LOGGER.error(e.getMessage());
			return null;
		}
	}
}
