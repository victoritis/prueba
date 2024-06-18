/*
 * Nombre: ServicioImpl
 * Descripción: Clase para poder comprar y anular billetes de tren.
 * Autores: Álvaro Villar, David Ibeas y Aitor Blanco.
 * Ver: 0.4.0
 */
package lsi.ubu.servicios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lsi.ubu.excepciones.CompraBilleteTrenException;
import lsi.ubu.util.PoolDeConexiones;

public class ServicioImpl implements Servicio {
	
	// Logger.
	private static final Logger LOGGER = LoggerFactory.getLogger(ServicioImpl.class);
	
	// Método que implementa la lógica de anular billetes de tren.
	@Override
	public void anularBillete(Time hora, java.util.Date fecha, String origen, String destino, int nroPlazas, int ticket)
			throws SQLException {
		
		// Obtenemos la instancia asociada al pool de conexiones.
		PoolDeConexiones pool = PoolDeConexiones.getInstance();
		
		/* Conversiones de fechas y horas */
		java.sql.Date fechaSqlDate = new java.sql.Date(fecha.getTime());
		java.sql.Timestamp horaTimestamp = new java.sql.Timestamp(hora.getTime());
		
		Connection con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		
		int precio;
		int idrecorrido;
		int idViaje;
		int nplazas;
		int plazasReservadas;
		
		// Dividimos el string de la hora para obtener solo la hora y los minutos.
		String horaDef = hora.toString().substring(0, 5);
		
		try {
			// Tomamos una conexión del pool de conexiones.
			con = pool.getConnection();
			
			// Definimos las consultas para obtener la información necesaria y realizar las actualizaciones necesarias.
			String select_viaje = "SELECT IDVIAJE, NPLAZASLIBRES " +
								  "FROM viajes a " +
								  "JOIN recorridos b ON a.IDRECORRIDO = b.IDRECORRIDO " +
								  "WHERE b.ESTACIONORIGEN = ? " +
                                  "AND b.ESTACIONDESTINO = ? " +
                                  "AND a.FECHA = ? " +
                                  "AND trunc(b.horaSalida) = trunc(?)";
			
			String select_ticket = "SELECT CANTIDAD FROM tickets WHERE IDTICKET = ?";
			String update_plazasLibres = "UPDATE viajes SET NPLAZASLIBRES = ? WHERE IDVIAJE = ?";
			String update_cantidadTicket = "UPDATE tickets SET CANTIDAD = ? WHERE IDTICKET = ?";
			String delete_ticket = "DELETE FROM tickets WHERE IDTICKET = ?";
			
			// Obtenemos el ID y el número de plazas libres del viaje.
			st = con.prepareStatement(select_viaje);
			st.setString(1, origen);
			st.setString(2, destino);
			st.setDate(3, fechaSqlDate);
			st.setTimestamp(4,horaTimestamp);
			rs = st.executeQuery();
			
			// Comprobamos que existe el viaje asociado.
			if (rs.next()) {
				idViaje = rs.getInt(1);
				nplazas = rs.getInt(2);
			} else {
				// creamos la excepción para poderla lanzas y guardar su mensaje
				CompraBilleteTrenException e=new CompraBilleteTrenException(CompraBilleteTrenException.NO_EXISTE_VIAJE);
				throw (e);//Lanzamos la excepción que hemos creado
			}
			
			// Obtenemos el número de plazas reservadas en el ticket.
			st = con.prepareStatement(select_ticket);
			st.setInt(1, ticket);
			rs=st.executeQuery();
			
			// Comprobamos si existe el ticket y si es posible anular ese número de plazas.
			if (rs.next()) {
				plazasReservadas = rs.getInt(1);
				if (nroPlazas > plazasReservadas) {
					// creamos la excepción para poderla lanzas y guardar su mensaje
					CompraBilleteTrenException e=new CompraBilleteTrenException(CompraBilleteTrenException.NO_RESERVAS);
					throw (e);//Lanzamos la excepción que hemos creado
				}
			} else {
				// creamos la excepción para poderla lanzas y guardar su mensaje
				CompraBilleteTrenException e=new CompraBilleteTrenException(CompraBilleteTrenException.NO_TICKET);
				throw (e);//Lanzamos la excepción que hemos creado
			}
			
			// Actualizamos el número de plazas libres del viaje.
			st = con.prepareStatement(update_plazasLibres);
			st.setInt(1, nplazas + nroPlazas);
			st.setInt(2, idViaje);
			st.executeUpdate();
			
			// Actualizamos la cantidad de plazas en el ticket.
			if(rs.getInt("cantidad")-nroPlazas>0){
			st = con.prepareStatement(update_cantidadTicket);
			st.setInt(1, nroPlazas);
			st.setInt(2, ticket);
			st.executeUpdate();
			}
			
			// Si el ticket se ha quedado sin plazas, procedemos a eliminarlo.
			else if (rs.getInt("cantidad")-nroPlazas==0) {
				st = con.prepareStatement(delete_ticket);
				st.setInt(1, ticket);
				st.executeUpdate();
			}
			
			// Hacemos commit para guardar los cambios.
			con.commit();
		} catch (SQLException e) {
			con.rollback();
			LOGGER.error(e.getMessage()); //Gardamos el mensaje de error en el logger
			st.close(); //cerramos la sentencia para liberar recursos
			con.close(); //cerramos la conexion para liberar recursos
			throw(e); //Volvemos a lanzar la excepci
		} finally {
			
			// Cerramos las conexiones y liberamos los recursos.
			if (rs != null) rs.close();
			if (st != null) st.close();
			if (con != null) con.close();
		}
	}
	
	// Método que implementa la lógica de comprar billetes de tren.
	@Override
	public void comprarBillete(Time hora, Date fecha, String origen, String destino, int nroPlazas)
			throws SQLException {
		PoolDeConexiones pool = PoolDeConexiones.getInstance();
		
		java.sql.Date fechaSqlDate = new java.sql.Date(fecha.getTime());
		java.sql.Timestamp horaTimestamp = new java.sql.Timestamp(hora.getTime());
		//SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		

		Connection con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		int precio;
		int idViaje;
		int nplazas;
		//Dividimos el string de la hora para obtener solo la hora y los minutos
		String horaDef=hora.toString().substring(0,5);
		con=pool.getConnection();//Tomamos una conexión del pool de conexiones
		try {
			//establecemos una sentencia preparada que unira las tablas de recorridos y viajes y enontrara el 
			//viaje que quiere el usuario y devuelve el precio, el id y el numero de plazas libres de viaje.
			st=con.prepareStatement("SELECT PRECIO,IDVIAJE,NPLAZASLIBRES FROM viajes a JOIN recorridos b ON a.IDRECORRIDO = b.IDRECORRIDO"
					+"WHERE b.ESTACIONORIGEN = ? AND b.ESTACIONDESTINO = ? AND a.FECHA = ? trunc(b.horaSalida) = trunc(?)");
			st.setString(1, origen);//rellenamos con la ciudad de origen que recibimos 
			st.setString(2, destino);//rellenamos con la ciudad de destino que recibimos 
			st.setDate(3,fechaSqlDate);//rellenamos con la fecha de salida que recibimos
			st.setTimestamp(4, horaTimestamp);//rellenamos con la hora que hemos recortado previamente
			rs=st.executeQuery(); //ejecutamos la sentencia
		}catch (SQLException e) {//en caso de que falle la ejecución de la sentencia  y salte un error sql
			con.rollback();//Hacemos rollback por que ha saltado una exepción
			LOGGER.error(e.getMessage()); //Gardamos el mensaje de error en el logger
			st.close(); //cerramos la sentencia para liberar recursos
			con.close(); //cerramos la conexion para liberar recursos
			throw(e); //Volvemos a lanzar la excepción
		}
		if(!rs.next()) {//Si el result set esta vacio significa que no existe un viaje con esos parametros
			con.rollback();//hacemos rollback ya que vamos a lanza una excepción
			// creamos la excepción para poderla lanzas y guardar su mensaje
			CompraBilleteTrenException e=new CompraBilleteTrenException(CompraBilleteTrenException.NO_EXISTE_VIAJE);
			LOGGER.error(e.getMessage());//Pasamos por el logger el mensaje de error
			st.close(); //cerramos la sentencia para liberar recursos
			con.close(); //cerramos la conexion para liberar recursos
			throw (e);//Lanzamos la excepción que hemos creado
			
		}else {//en caso de que haya algo en el result set
			
			precio=rs.getInt(1);//Guardamos el precio viaje
			idViaje=rs.getInt(2); //Guardamos el id del viaje 
			nplazas=rs.getInt(3); //Guardamos el numero de plazas del viaje 
			if(nplazas>=nroPlazas) {//Si hay suficiente plazas para reservar
				try {
				nplazas=nplazas-nroPlazas;// restamos el numero de plazas que queremos reservar  
				//preparamos una sentencia para actualizar el numero de plazas libres del viaje que hemos cogido
				st=con.prepareStatement("UPDATE viajes SET NPLAZASLIBRES = ? WHERE IDVIAJE=?");
				st.setInt(1, nplazas);//el nuevo numero de plazas libres
				st.setInt(2, idViaje); //introducimos el id del viaje que se ha seleccionado
				st.executeUpdate();//Ejecutamos la actualización de la tabla
				//preparamos una sentencia para añadir la nueva compra de ticket
				st=con.prepareStatement("insert into tickets values(seq_tickets.nextval,?,CURRENT_DATE,?,?)");
				st.setInt(1, idViaje);//introducimo el id del viaje que hemos cromprado
				st.setInt(2,nroPlazas); //introducimos cuantas plazas hemos cogido
				precio=precio*nroPlazas; //obtenemos el precio del ticket real ya que el precio anterior era el precio por plaza
				st.setInt(3, precio);//introducimos el precio total
				st.executeUpdate(); //ejecutamos la actualización de la tabla
				con.commit(); //comitemaos los cambios
				}catch(SQLException e) {//en caso de que falle la ejecución de la sentencia  y salte un error sql
					con.rollback();//Hacemos rollback por que ha saltado una exepción
					LOGGER.error(e.getMessage()); //Gardamos el mensaje de error en el logger
					st.close(); //cerramos la sentencia para liberar recursos
					con.close(); //cerramos la conexion para liberar recursos
					throw(e); //Volvemos a lanzar la excepción
				}
			}else {
				con.rollback();//hacemos rollback ya que vamos a lanza una excepción
				// creamos la excepción para poderla lanzas y guardar su mensaje
				CompraBilleteTrenException e=new CompraBilleteTrenException(CompraBilleteTrenException.NO_PLAZAS);
				LOGGER.error(e.getMessage());//Pasamos por el logger el mensaje de error
				st.close(); //cerramos la sentencia para liberar recursos
				con.close(); //cerramos la conexion para liberar recursos
				throw (e);//Lanzamos la excepción que hemos creado
			}
			
		}
			st.close();//cerramos la sentencia para liberar recursos
			con.close(); //cerramos la conexión para liberar recursos
		
		
	}
}
