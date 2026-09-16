package com.marwix127.court_booking;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Postgres real para los tests, no H2.
 *
 * Es imprescindible aqui: el nucleo del dominio es la restriccion EXCLUDE de
 * booking_no_overlap, que H2 no soporta. Contra H2 los tests de solape
 * pasarian sin comprobar nada.
 *
 * Publica para que la puedan importar tests de otros paquetes.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	/**
	 * Version fijada, no 'latest': con latest el mismo commit puede pasar hoy
	 * y fallar manyana porque cambio la imagen. Es la misma major que usa
	 * docker-compose en desarrollo.
	 */
	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
	}

}
