package org.bonitasoft.store.toolbox;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

public class LoggerStore {

	static public Logger logger = Logger.getLogger(LoggerStore.class.getName());

	public enum LOGLEVEL {
		NOLOG(0), ERROR(1), MAIN(2), INFO(3), DEBUG(4);

		int ord;

		private LOGLEVEL(final int o) {
			ord = o;
		};
	};

	/**
	 * 0 => no Log 1 => Only MAINLOG : start / end 2 => Log INFO 3 => Log DEBUG
	 */
	public LOGLEVEL logLevel = LOGLEVEL.INFO;

	public boolean isLog(final LOGLEVEL levelMessage) {
		return levelMessage.ord <= logLevel.ord;
	}

	public void log(final LOGLEVEL levelMessage, final String message) {
		if (isLog(levelMessage)) {
			logger.info(message);
		}
	}
	public void info(String message )
	{
	  log( LOGLEVEL.INFO, message);
	}

	public void severe(String message )
	{
	  log( LOGLEVEL.ERROR, message);
	}
	/**
	 * log a message exception
	 * 
	 * @param message
	 * @param e
	 */
	public void logException(final String message, final Exception e) {
		final StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		log(LOGLEVEL.ERROR, message + " : " + sw);
	}

}
