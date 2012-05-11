package de.prob;

import static java.io.File.*;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.animator.IAnimator;
import de.prob.cli.ProBInstanceProvider;

public class Main {

	private static Injector INJECTOR;

	public IAnimator getAnimator() {
		return INJECTOR.getInstance(IAnimator.class);
	}

	private final CommandLineParser parser;
	private final Options options;
	private final Shell shell;
	public final static String PROB_HOME = getProBDirectory();
	public final static String LOG_CONFIG = System
			.getProperty("PROB_LOG_CONFIG") == null ? "production.xml" : System
			.getProperty("PROB_LOG_CONFIG");

	@Inject
	public Main(final CommandLineParser parser, final Options options,
			final Shell shell) {
		this.parser = parser;
		this.options = options;
		this.shell = shell;
	}

	void run(final String[] args) {
		getAnimator();
		if (ProBInstanceProvider.getClis().isEmpty())
			System.out
					.println("No cli detected. Try \"upgrade\" to download the current version.");

		try {
			CommandLine line = parser.parse(options, args);
			if (line.hasOption("shell")) {
				shell.repl();
			}
			if (line.hasOption("test")) {
				String value = line.getOptionValue("test");
				shell.runScript(new File(value));
			}
		} catch (ParseException exp) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar probcli.jar", options);
		}
	}

	public static String getProBDirectory() {
		String homedir = System.getProperty("prob.home");
		if (homedir != null)
			return homedir + separator;
		String env = System.getenv("PROB_HOME");
		if (env != null)
			return env + separator;
		return System.getProperty("user.home") + separator + ".prob"
				+ separator;
	}

	public static void main(final String[] args) {

		System.setProperty("PROB_LOG_CONFIG", LOG_CONFIG);
		System.setProperty("PROB_LOGFILE", PROB_HOME + "logs" + separator
				+ "ProB.txt");

		INJECTOR = Guice.createInjector(new MainModule());
		Main main = INJECTOR.getInstance(Main.class);

		main.run(args);
		System.exit(0);
	}

}