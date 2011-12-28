package de.prob;

import static java.io.File.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.script.ScriptEngineManager;

import jline.ConsoleReader;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import de.prob.animator.AnimatorModule;
import de.prob.annotations.Home;
import de.prob.annotations.Language;
import de.prob.annotations.Logfile;
import de.prob.annotations.Version;
import de.prob.cli.ModuleCli;
import de.prob.model.ModelModule;
import de.prob.scripting.Api;

public class MainModule extends AbstractModule {

	private static final Logger logger = LoggerFactory
			.getLogger(MainModule.class);
	private final Properties buildConstants;

	public MainModule() {
		buildConstants = loadBuildConstants();
	}

	@Override
	protected void configure() {
		install(new ModuleCli());
		install(new AnimatorModule());
		install(new ModelModule());
		bind(Api.class);
		bind(CommandLineParser.class).to(PosixParser.class).in(Singleton.class);
		bind(String.class).annotatedWith(Language.class).toInstance("groovy");
		bind(String.class).annotatedWith(Version.class).toInstance(
				buildConstants.getProperty("version", "0.0.0"));
		bind(ConsoleReader.class);
		bind(ClassLoader.class).annotatedWith(Names.named("Shell")).toInstance(
				Shell.class.getClassLoader());
		bind(Shell.class);
		bind(ScriptEngineManager.class);
	}

	@Provides
	@Home
	public String getProBDirectory() {
		return System.getProperty("user.home") + separator + ".prob"
				+ separator;
	}

	@Provides
	@Logfile
	public String getProBLogfile() {
		return getProBDirectory() + "logs" + separator + "ProB.txt";
	}

	@Provides
	public Options getCommandlineOptions() {
		Options options = new Options();
		Option shell = new Option("s", "shell", false,
				"start ProB-Python shell");
		Option modelcheck = new Option("mc", "modelcheck", false,
				"start ProB model checking");
		OptionGroup mode = new OptionGroup();
		mode.setRequired(true);
		mode.addOption(modelcheck);
		mode.addOption(shell);
		options.addOptionGroup(mode);
		return options;
	}

	private Properties loadBuildConstants() {
		InputStream stream = MainModule.class.getClassLoader()
				.getResourceAsStream("build.properties");
		Properties properties = new Properties();
		try {
			properties.load(stream);
		} catch (IOException e) {
			logger.debug("Could not load build.properties.", e);
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				logger.debug("Could not close stream.", e);
			}
		}
		return properties;
	}

}