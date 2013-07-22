package com.tpeter.loadtest.jmeter.sampler.file;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Store file and it's position in the input folder
 * 
 * @author Peter
 *
 */
public final class RequestBodyFile {
	private File file;
	private int index;

	public RequestBodyFile(File file, int index) {
		this.file = file;
		this.index = index;
	}
	
	public File getFile() {
		return file;
	}
	
	public int getIndex() {
		return index;
	}
	
	public Path getFileAsPath() {
		return Paths.get(file.toURI());
	}
	

}
