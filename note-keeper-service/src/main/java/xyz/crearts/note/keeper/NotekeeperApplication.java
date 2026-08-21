package xyz.crearts.note.keeper;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.scheduling.annotation.EnableScheduling;
import xyz.crearts.note.keeper.config.NativeRuntimeHints;

@SpringBootApplication
@MapperScan(basePackages = "xyz.crearts.note.keeper.mapper", sqlSessionTemplateRef = "sqlSessionTemplate")
@EnableScheduling
@ImportRuntimeHints(NativeRuntimeHints.class)
public class NotekeeperApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotekeeperApplication.class, args);
	}

}
