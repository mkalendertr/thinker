package edu.yeditepe.controller;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import edu.yeditepe.lucene.EntityIndexer;

@RequestMapping("/api/rest")
@RestController
public class IndexController {
	private static final Logger LOGGER = Logger
			.getLogger(IndexController.class);

	@RequestMapping(value = "/index", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
	public @ResponseBody String index() {
		EntityIndexer indexer = new EntityIndexer();
		try {
			indexer.rebuildIndexes();
		} catch (IOException e) {
			LOGGER.error(e);
		}
		return "Finished";
	}

}
