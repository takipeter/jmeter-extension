package com.tpeter.loadtest.jmeter.sampler;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import com.tpeter.loadtest.jmeter.sampler.file.RequestBodyFile;

/**
 * JMeter sampler for sending random requests to a host
 * 
 * It picks up a random request body file from the input folder and
 * construct the request sending to the host.
 * 
 * Usage in JMeter: create a jar from this file and put into jmeter\lib\ext
 * 
 * Input from JMeter UI: 
 *  - protocol
 *  - address
 *  - path
 *  - input_dir
 * 
 * @author Peter
 *
 */
public class PostRequestSampler extends AbstractJavaSamplerClient implements
		Serializable {
	
	private static final String RESPONSE_CODE_500 = "500";
	private final Random random = new Random();
	private static final Set<Integer> processingFileIndexSet = Collections.synchronizedSet(new LinkedHashSet<Integer>());
	
	private boolean useQueue = true;
	
	/**
	 * Set up default arguments for the JMeter GUI
	 */
	@Override
	public Arguments getDefaultParameters() {
		Arguments defaultParameters = new Arguments();
		defaultParameters.addArgument("PROTOCOL", "http");
		defaultParameters.addArgument("ADDRESS", "localhost:80");
		defaultParameters.addArgument("PATH", "/testServlet");
		defaultParameters.addArgument("INPUT_DIR", "${__P(INPUT_DIR}");
		defaultParameters.addArgument("USE_QUEUE", "true");

		return defaultParameters;
	}

	/**
	 * Custom test runner
	 */
	@Override
	public SampleResult runTest(JavaSamplerContext context) {
		
		String protocolParam = context.getParameter("PROTOCOL");
		String serverParam = context.getParameter("ADDRESS");
		String pathParam = context.getParameter("PATH");
		String dirParam = context.getParameter("INPUT_DIR");
		useQueue = Boolean.parseBoolean(context.getParameter("USE_QUEUE"));
		
		SampleResult result = new SampleResult();
		
		HttpURLConnection connection = null;
		DataOutputStream wr = null;
		InputStream is = null;
		BufferedReader rd = null;
		RequestBodyFile file = null;
		try {
			file = getRandomFile(dirParam);
			byte[] data = Files.readAllBytes(file.getFileAsPath());
			result.setThreadName(result.getThreadName() + " - " + file.getFileAsPath().getFileName().toString());

			// start watch
			result.sampleStart();

			URL url = new URL(protocolParam + "://" + serverParam + pathParam);
			connection = (HttpURLConnection) url.openConnection();
			connection.setFixedLengthStreamingMode(data.length);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type",
					"application/octet-stream");
			connection.setRequestProperty("Content-Length",
					Integer.toString(data.length));
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// send request
			wr = new DataOutputStream(
					connection.getOutputStream());
			wr.write(data);
			wr.flush();
			wr.close();
			
			// get Response	
			is = connection.getInputStream();
			rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			is.close();

			// stop watch
			result.sampleEnd();

			if (connection.getResponseCode() == 200) {				
				result.setSuccessful(true);
			}
			else {
				result.setSuccessful(false);
			}
			
			result.setResponseMessage(connection.getResponseMessage());
			result.setResponseCode(Integer.toString(connection.getResponseCode()));
			result.setResponseData(response.toString());
			result.setDataType(SampleResult.TEXT);
			
		} catch (Exception e) {
			result.sampleEnd(); // stop stopwatch
			result.setSuccessful(false);
			result.setResponseMessage("Exception: " + e);

			// get stack trace as a String to return as document data
			StringWriter stringWriter = new StringWriter();
			e.printStackTrace(new PrintWriter(stringWriter));
			result.setResponseData(stringWriter.toString());
			result.setDataType(org.apache.jmeter.samplers.SampleResult.TEXT);
			result.setResponseCode(RESPONSE_CODE_500);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
			if (rd != null) {
				try {
					rd.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (wr != null) {
				try {
					wr.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// enable processing for other threads as well
			if (useQueue) {
				processingFileIndexSet.remove(file.getIndex());
			}
		}

		return result;
	}

	protected RequestBodyFile getRandomFile(String dirParam) {
		Path baseDir = Paths.get(dirParam);
		
		// TODO: Peter: this is ugly here, do proper validation.
		if (Files.notExists(baseDir)) {
			// exit from JVM, jmeter process it
			// set jmeterengine.stopfail.system.exit=true in jmter.properties
			System.exit(1);
		}
		
		File[] files = baseDir.toFile().listFiles();	
		int fileNum = files.length;
		
		if (0 == fileNum) {
			// exit from JVM, jmeter process it
			// set jmeterengine.stopfail.system.exit=true in jmter.properties
			System.exit(1);
		}
		
			
		Integer fileIndex = 0;
		do {
			fileIndex = random.nextInt(fileNum);
		} while (useQueue && !processingFileIndexSet.add(fileIndex));
		
		File testFile = files[fileIndex];	
		RequestBodyFile file = new RequestBodyFile(testFile, fileIndex);
		
		return file;
	}
}
