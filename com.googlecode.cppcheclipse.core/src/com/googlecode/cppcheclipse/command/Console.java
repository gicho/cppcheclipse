package com.googlecode.cppcheclipse.command;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import com.googlecode.cppcheclipse.core.CppcheclipsePlugin;

/**
 * Wrapper around a console window, which can output an existing InputSteam.
 * @author Konrad Windszus
 *
 */
public class Console {

	private static final String NAME = "cppcheck";
	private final MessageConsole console;

	public Console() {
		console = findConsole(NAME);
	}

	private static MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName()))
				return (MessageConsole) existing[i];
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}

	public ConsoleInputStream createInputStream(InputStream input) {
		return new ConsoleInputStream(input);
	}
	
	public ConsoleByteArrayOutputStream createByteArrayOutputStream() {
		return new ConsoleByteArrayOutputStream();
	}

	public void print(String line) throws IOException {
		final MessageConsoleStream output = console.newMessageStream();
		output.print(line);
		output.close();
	}
	public void println(String line) throws IOException {
		final MessageConsoleStream output = console.newMessageStream();
		output.println(line);
		output.close();
	}

	public void show() throws PartInitException {
		IWorkbenchPage page = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		String id = IConsoleConstants.ID_CONSOLE_VIEW;
		IConsoleView view = (IConsoleView) page.showView(id);
		view.display(console);

	}
	
	public class ConsoleByteArrayOutputStream extends ByteArrayOutputStream {
		private final MessageConsoleStream output;

		private static final int BYTE_ARRAY_INITIAL_SIZE = 4096;
		
		public ConsoleByteArrayOutputStream() {
			this(BYTE_ARRAY_INITIAL_SIZE);
		}
		
		public ConsoleByteArrayOutputStream(int size) {
			super(size);
			output = console.newMessageStream();
		}

		@Override
		public synchronized void write(byte[] b, int off, int len) {
			try {
				output.write(b, off, len);
			} catch (IOException e) {
				CppcheclipsePlugin.log(e);
			}
			super.write(b, off, len);
		}

		@Override
		public synchronized void write(int b) {
			try {
				output.write(b);
			} catch (IOException e) {
				CppcheclipsePlugin.log(e);//CppcheckProcess.log(e);
			}
			super.write(b);
		}

		public void print(String line) {
			output.println(line);
		}

		@Override
		public void close() throws IOException {
			super.close();
			output.close();
		}
	}

	public class ConsoleInputStream extends FilterInputStream {
		private final MessageConsoleStream output;

		public ConsoleInputStream(InputStream input) {
			super(input);
			output = console.newMessageStream();
		}

		@Override
		public int read() throws IOException {
			int result = super.read();
			if (result != -1) {
				output.write(result);
			}
			return result;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int result = super.read(b, off, len);
			if (result > 0) {
				output.write(b, off, result);
			}
			return result;
		}

		public void print(String line) {
			output.println(line);
		}

		@Override
		public void close() throws IOException {
			super.close();
			output.close();
		}
	}
}