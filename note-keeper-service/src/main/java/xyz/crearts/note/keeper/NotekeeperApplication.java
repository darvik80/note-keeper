package xyz.crearts.note.keeper;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@MapperScan("xyz.crearts.note.keeper.mapper")
@EnableScheduling
public class NotekeeperApplication {

	public static void main(String[] args) throws Exception {
		Files.createDirectories(Path.of(System.getProperty("user.dir"), "data"));
		SpringApplication.run(NotekeeperApplication.class, args);
	}

}
