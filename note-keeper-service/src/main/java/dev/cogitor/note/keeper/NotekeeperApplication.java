package dev.cogitor.note.keeper;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.scheduling.annotation.EnableScheduling;
import dev.cogitor.note.keeper.config.NativeRuntimeHints;

@SpringBootApplication
@MapperScan(basePackages = "dev.cogitor.note.keeper.mapper", sqlSessionTemplateRef = "sqlSessionTemplate")
@EnableScheduling
@ImportRuntimeHints(NativeRuntimeHints.class)
public class NotekeeperApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotekeeperApplication.class, args);
	}

}
