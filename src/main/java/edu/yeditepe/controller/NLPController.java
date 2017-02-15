package edu.yeditepe.controller;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import edu.yeditepe.nlp.ITUNLP;

@RequestMapping("/nlp")
@RestController
public class NLPController {
	private static final Logger LOGGER = Logger.getLogger(NLPController.class);

	@RequestMapping(value = "/itu", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
	public @ResponseBody String itu(@RequestParam(value = "text") String text) {
		String result = ITUNLP.getInstance().pipeline(text);
		return result;
	}

}
