package com.bmskinner.nuclear_morphology.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class ConsoleHandler extends StreamHandler {
	
	public ConsoleHandler(Formatter formatter) {
		super();
		this.setFormatter(formatter);
	}
	

	@Override
	public void publish(LogRecord record) {
		System.out.println(getFormatter().format(record));

	}

}
