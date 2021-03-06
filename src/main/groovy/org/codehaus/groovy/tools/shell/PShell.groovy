/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.groovy.tools.shell

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

import jline.History
import jline.Terminal

import org.codehaus.groovy.runtime.StackTraceUtils
import org.codehaus.groovy.tools.shell.util.MessageSource
import org.codehaus.groovy.tools.shell.util.Preferences
import org.codehaus.groovy.tools.shell.util.XmlCommandRegistrar
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole

import de.prob.cli.ProBInstanceProvider
import de.prob.scripting.Api


/**
 * An interactive shell for evaluating Groovy code from the command-line (aka. groovysh).
 *
 * @version $Id: PShell.groovy 21580 2011-02-17 20:06:49Z glaforge $
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
class PShell
extends Shell {
	static {
		// Install the system adapters
		AnsiConsole.systemInstall()

		// Register jline ansi detector
		Ansi.setDetector(new AnsiDetector())
	}

	private static final MessageSource messages = new MessageSource(Groovysh.class)

	private static final long serialVersionUID = -9047891508993732223L;

	final BufferManager buffers = new BufferManager()

	final Parser parser

	final Interpreter interp

	final List imports = []

	final version

	PInteractiveShellRunner runner

	History history

	boolean historyFull  // used as a workaround for GROOVY-2177
	String evictedLine  // remembers the command which will get evicted if history is full

	private final Binding binding;
	PShell(final ClassLoader classLoader, final Binding binding, final IO io, final Closure registrar, String version) {
		super(io)

		this.binding =  binding;
		assert classLoader
		assert binding
		assert registrar

		parser = new Parser()

		interp = new Interpreter(classLoader, binding)
		this.version = version
		registrar.call(this)
	}


	PShell(final ClassLoader classLoader, final Binding binding, final IO io, String version) {
		this(classLoader, binding, io, { shell ->
			def r = new XmlCommandRegistrar(shell, classLoader)
			def resource = de.prob.Main.class.getClassLoader().getResource('commands.xml')
			r.register(resource)
		}, version)
	}

	Api getApi() {
		return binding.getVariable("api"); }

	/**
	 * Execute a single line, where the line may be a command or Groovy code (complete or incomplete).
	 */
	Object execute(final String line) {
	 return execute(line,false);	
	}
	
	Object execute(final String line, boolean silent) {
		assert line != null

		// Ignore empty lines
		if (line.trim().size() == 0) {
			return null
		}

		maybeRecordInput(line)

		def result

		// First try normal command execution
		if (isExecutable(line)) {
			result = executeCommand(line)

			// For commands, only set the last result when its non-null/true
			if (result) {
				lastResult = result
			}

			return result
		}

		// Otherwise treat the line as Groovy
		def current = []
		current += buffers.current()

		// Append the line to the current buffer
		current << line

		// Attempt to parse the current buffer
		def status = parser.parse(imports + current)

		switch (status.code) {
			case ParseCode.COMPLETE:
				log.debug("Evaluating buffer...")

				if (io.verbose) {
					displayBuffer(buffer)
				}

			// Evaluate the current buffer w/imports and dummy statement
				def buff = imports + ['true']+ current



				def ex = Executors.newSingleThreadExecutor();

				Future<Object> r = ex.submit(new Callable<Object>() {
							public Object call() {
								def oldbindings = new HashSet();
								oldbindings.addAll(binding.getVariables().keySet())
								result = interp.evaluate(buff);
								def newbindings = binding.getVariables().keySet()
								newbindings.each {if (!oldbindings.contains(it) && !it.startsWith("this") && !it.startsWith("__"))  { println it; }}
								return result
							}});

			//				lastResult = result = interp.evaluate(buff)

				while (!r.isDone()) {
					def c = System.in.available()
					if (c >0) {
						def a = System.in.read()
						if( a == 97 ) {
							def b = System.in.read()
							if( b == 10 ) {
								r.cancel(true)
								ProBInstanceProvider.getClis().each {
									if (it.get() != null) it.get().sendInterrupt()
								}
								ex.shutdown();
							} else {
								print(Character.toChars(b))
							}
						} else {
							print(Character.toChars(a))
						}

					}
				}

				lastResult = result = (r.isCancelled() ? "CANCELED" : r.get());

				buffers.clearSelected()
				break

			case ParseCode.INCOMPLETE:
			// Save the current buffer so user can build up complex multi-line code blocks
				buffers.updateSelected(current)
				break

			case ParseCode.ERROR:
				throw status.cause

			default:
			// Should never happen
				throw new Error("Invalid parse status: $status.code")
		}

		return result
	}

	protected Object executeCommand(final String line) {
		return super.execute(line)
	}

	/**
	 * Display the given buffer.
	 */
	private void displayBuffer(final List buffer) {
		assert buffer

		buffer.eachWithIndex { line, index ->
			def lineNum = formatLineNumber(index)

			io.out.println(" ${lineNum}  >  $line")
		}
	}

	//
	// Prompt
	//

	private String renderPrompt() {
		def lineNum = formatLineNumber(buffers.current().size())

		//return AnsiRenderer.render("@|bold ProB:|@${lineNum}@|bold >|@ ")
		return "ProB:"+lineNum+"> ";
	}

	/**
	 * Format the given number suitable for rendering as a line number column.
	 */
	private String formatLineNumber(final int num) {
		assert num >= 0

		// Make a %03d-like string for the line number
		return num.toString().padLeft(3, '0')
	}

	//
	// User Profile Scripts
	//

	File getUserStateDirectory() {
		def userHome = new File(System.getProperty('user.home'))
		def dir = new File(userHome, '.groovy')
		return dir.canonicalFile
	}


	private void loadUserScript(final String filename) {
		assert filename

		def file = new File(userStateDirectory, filename)

		if (file.exists()) {
			def command = registry['load']

			if (command) {
				log.debug("Loading user-script: $file")

				// Disable the result hook for profile scripts
				def previousHook = resultHook
				resultHook = { result -> /* nothing */}

				try {
					command.load(file.toURI().toURL())
				}
				finally {
					// Restore the result hook
					resultHook = previousHook
				}
			}
			else {
				log.error("Unable to load user-script, missing 'load' command")
			}
		}
	}

	//
	// Recording
	//

	private void maybeRecordInput(final String line) {
		def record = registry['record']

		if (record != null) {
			record.recordInput(line)
		}
	}

	private void maybeRecordResult(final Object result) {
		def record = registry['record']

		if (record != null) {
			record.recordResult(result)
		}
	}

	private void maybeRecordError(Throwable cause) {
		def record = registry['record']

		if (record != null) {
			boolean sanitize = Preferences.sanitizeStackTrace

			if (sanitize) {
				cause = StackTraceUtils.deepSanitize(cause);
			}

			record.recordError(cause)
		}
	}

	//
	// Hooks
	//

	final Closure defaultResultHook = { result ->
		boolean showLastResult = !io.quiet && (io.verbose || Preferences.showLastResult)

		if (showLastResult) {
			// Need to use String.valueOf() here to avoid icky exceptions causes by GString coercion
			io.out.println(String.valueOf(result))
		}
	}

	Closure resultHook = defaultResultHook

	private void setLastResult(final Object result) {
		if (resultHook == null) {
			throw new IllegalStateException("Result hook is not set")
		}

		resultHook.call((Object)result)

		interp.context['_'] = result

		maybeRecordResult(result)
	}

	private Object getLastResult() {
		return interp.context['_']
	}

	final Closure defaultErrorHook = { Throwable cause ->
		assert cause != null

		io.err.println("ERROR ${cause.class.name}:")
		io.err.println("  ${cause.message}")

		maybeRecordError(cause)

		if (log.debug) {
			// If we have debug enabled then skip the fancy bits below
			log.debug(cause)
		}
		else {
			boolean sanitize = Preferences.sanitizeStackTrace

			// Sanitize the stack trace unless we are in verbose mode, or the user has request otherwise
			if (!io.verbose && sanitize) {
				cause = StackTraceUtils.deepSanitize(cause);
			}

			def trace = cause.stackTrace

			def buff = new StringBuffer()

			for (e in trace) {
				buff << "         at ${e.className}.${e.methodName} ( "

				buff << (e.nativeMethod ? 'Native Method' :
						(e.fileName != null && e.lineNumber != -1 ? "${e.fileName}:${e.lineNumber}" :
						(e.fileName != null ? e.fileName : 'Unknown Source')))

				buff << ')'

				io.err.println(buff)

				buff.setLength(0) // Reset the buffer

				// Stop the trace once we find the root of the evaluated script
				if (e.className == Interpreter.SCRIPT_FILENAME && e.methodName == 'run') {
					io.err.println('         ...')
					break
				}
			}
		}
	}

	Closure newErrorHook = { Throwable cause ->
		//		if(!(cause instanceof ProBException))
		defaultErrorHook.call(cause)
	}

	//Closure errorHook = defaultErrorHook
	Closure errorHook = newErrorHook

	private void displayError(final Throwable cause) {
		if (errorHook == null) {
			throw new IllegalStateException("Error hook is not set")
		}

		errorHook.call(cause)
	}

	//
	// Interactive Shell
	//

	int run(final String[] args) {
		String commandLine = null

		if (args != null && args.length > 0) {
			commandLine = args.join(' ')
		}

		return run(commandLine as String)
	}

	int run(final String commandLine) {
		def term = Terminal.terminal

		if (log.debug) {
			log.debug("Terminal ($term)")
			log.debug("    Supported:  $term.supported")
			log.debug("    ECHO:       $term.echo (enabled: $term.echoEnabled)")
			log.debug("    H x W:      $term.terminalHeight x $term.terminalWidth")
			log.debug("    ANSI:       ${term.isANSISupported()}")

			if (term instanceof jline.WindowsTerminal) {
				log.debug("    Direct:     ${term.directConsole}")
			}
		}

		def code

		try {



				// Setup the interactive runner
				runner = new PInteractiveShellRunner(this, this.&renderPrompt as Closure)

				// Setup the history
				runner.history = history = new History()
				runner.historyFile = new File(userStateDirectory, 'groovysh.history')

				// Setup the error handler
				runner.errorHandler = this.&displayError

				//
				// TODO: See if we want to add any more language specific completions, like for println for example?
				//

				// Display the welcome banner
				if (!io.quiet) {
					def width = term.terminalWidth

					// If we can't tell, or have something bogus then use a reasonable default
					if (width < 1) {
						width = 80
					}

					//io.out.println(messages.format('startup_banner.0', InvokerHelper.version, System.properties['java.version']))
					io.out.println("ProB Shell "+version)
					io.out.println("Type 'help' for help.")
					io.out.println('-' * (width - 1))
				}

				// initial code goes here
				// execute("", true) 
				
				// And let 'er rip... :-)
				runner.run()



			code = 0
		}
		catch (ExitNotification n) {
			log.debug("Exiting w/code: ${n.code}")

			code = n.code
		}
		catch (Throwable t) {
			io.err.println(messages.format('info.fatal', t))
			t.printStackTrace(io.err)

			code = 1
		}

		assert code != null // This should never happen

		return code
	}

	//can be used to define new variables that are recognized throughout the whole shell
	void add(String s) {
		this << s
	}

	void addVariable(String name, Object obj) {
		binding.setVariable(name, obj);
	}
}
