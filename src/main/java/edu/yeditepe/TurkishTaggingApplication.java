package edu.yeditepe;

import java.nio.charset.Charset;

import org.apache.catalina.connector.Connector;
import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.filter.CharacterEncodingFilter;

import edu.yeditepe.nlp.ITUNLP;
import edu.yeditepe.utils.Property;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class TurkishTaggingApplication {
	private static final Logger LOGGER = Logger
			.getLogger(TurkishTaggingApplication.class);

	@Bean
	public HttpMessageConverter<String> responseBodyConverter() {
		return new StringHttpMessageConverter(Charset.forName("UTF-8"));
	}

	@Bean
	public CharacterEncodingFilter characterEncodingFilter() {
		final CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
		characterEncodingFilter.setEncoding("UTF-8");
		characterEncodingFilter.setForceEncoding(true);
		return characterEncodingFilter;
	}

	@Bean
	public EmbeddedServletContainerFactory servletContainer() {
		TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory(
				Property.getInstance().getInt("server.port"));
		factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {

			@Override
			public void customize(Connector connector) {
				connector.setURIEncoding("UTF-8");

			}
		});
		return factory;
	}

	public static void main(String[] args) {
		SpringApplication.run(TurkishTaggingApplication.class, args);
	}

	public void run(String... args) throws Exception {
		ITUNLP.getInstance().disambiguate(
				"Yeditepe Üniversitesi, İstanbul'da bulunuyor.");
		// test();
		// processWikipedia();

	}

	public void test() {
		// String input =
		// "Huawei, dünyanın en büyük 50 operatörünün 36'sının tercih ettiği, yeni nesil telekomünikasyon teknolojilerinde öncü bir firmadır.";
		// String input =
		// "Yeditepe Üniversitesi harç ücretlerine 5 lira zam yaptı.";
		// String input =
		// "Araştırmamıza katılan avukatların % 65'i erkektir, % 34'ü kadındır. Araştırmamıza katılan 280 kişiden 183'ü erkek ve 97'si kadındır.";
		// List<Sentence> sentences = ITUNLP.getInstance().process(input,
		// repository);
		// LOGGER.info("Test insert finish");
		// repository.deleteAll();
		// fetch all Sentences
		// LOGGER.info("Sentence found with findAll():");
		// LOGGER.info("-------------------------------");
		// for (Sentence s : repository.findAll()) {
		// LOGGER.info(s);
		// }
		//
		// // fetch an individual Sentence
		// LOGGER.info("Sentence found with findByOrignalText('Yeditepe Üniversitesi'):");
		// LOGGER.info("--------------------------------");
		// LOGGER.info(repository.findByOrignalText("Yeditepe Üniversitesi"));

	}

	/**
	 * @param args
	 */
	public void processWikipedia() {
		// String bz2Filename =
		// "C:\\wikipedia_turkish\\trwiki-latest-pages-articles.xml";
		//
		// Wikipedia wikipedia = new Wikipedia(bz2Filename, repository,
		// ITUNLP.getInstance());
		// wikipedia.process();
		// LOGGER.info("");
	}

}
