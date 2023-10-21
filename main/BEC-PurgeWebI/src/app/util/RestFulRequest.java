package app.util;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

//@SuppressWarnings("nls")
public class RestFulRequest {

	private static boolean trace = true;
	private String requestUrl, requestMethod, requestContent, responseContent, responseMessage;
	private int responseCode;
	private Map<String, List<String>> responseHeaders;
	private String logonToken;

	public int getResponseCode() {
		return this.responseCode;
	}

	public String getResponseMessage() {
		return this.responseMessage;
	}

	public String getResponseContent() {
		return this.responseContent;
	}

	public Map<String, List<String>> getResponseHeaders() {
		return this.responseHeaders;
	}

	public static boolean isTrace() {
		return trace;
	}

	public static void setTrace(boolean trace) {
		RestFulRequest.trace = trace;
	}

	// Constructor
	public RestFulRequest() {
	}

	// Constructor
	public RestFulRequest(String logonToken) {
		this.logonToken = logonToken;
	}

	/**
	 * @see #send(String, String, String, String, String)
	 */
	public void send(String url, String method, String xmlContent) throws Exception {
		send(url, method, xmlContent, null, "application/xml");
	}

	/**
	 * Utility method to send HTTP requests.
	 * <p>
	 * It provides the following features:
	 * <ul>
	 * <li>Handling SAP-specific headers, such as X-SAP-LogonToken</li>
	 * <li>Allowing to add XML content to the request</li>
	 * <li>Reading the server response, available via the static members
	 * <i>responseContent</i>, <i>responseCode</i> and <i>responseMessage</i></li>
	 * <li>Showing the request and response in the console by setting the static
	 * member <i>trace</i> value to <i>true</i></li>
	 * </ul>
	 * </p>
	 * 
	 * @param url			The URL to send
	 * @param method		The HTTP request method (GET, POST, PUT or DELETE)
	 * @param contentXml	The XML content to send or <i>null</i> if nothing has to be sent
	 * @param contentType	Accept 
	 * @param accept		Accept header value (requested format for the response)
	 * @throws Exception
	 */
	@SuppressWarnings("resource")
	public void send(String url, String method, String contentXml, String contentType, String accept) 
			throws Exception {
		this.reset();
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestMethod(method);

		if (accept != null) {
			connection.setRequestProperty("Accept", accept);
		}

		if (this.logonToken != null) {
			connection.setRequestProperty("X-SAP-LogonToken", this.logonToken);
		}

		if (contentXml != null) {
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", contentType);
			connection.setRequestProperty("Content-Length", String.valueOf(contentXml.getBytes().length));

			DataOutputStream out = new DataOutputStream(connection.getOutputStream());
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
			writer.write(contentXml);
			writer.close();
			out.close();

//			DataOutputStream out = new DataOutputStream(connection.getOutputStream());
//			out.writeBytes(contentXml);
//			out.flush();
//			out.close();
		}

		// Reads response
		InputStream in;
		try {
			in = connection.getInputStream();
		} catch (IOException e) {
			in = connection.getErrorStream();
		}
		if (in == null) {
			throw new Exception("Connection to " + url + " failed");
		}

		// Fix UTF-8 issue
		Scanner scanner = new Scanner(in, "UTF-8").useDelimiter("\\A");

		this.requestUrl = url;
		this.requestMethod = method;
		this.requestContent = contentXml;
		this.responseContent = scanner.hasNext() ? scanner.next() : "";
		this.responseCode = connection.getResponseCode();
		this.responseMessage = connection.getResponseMessage();
		this.responseHeaders = connection.getHeaderFields();

		if (trace)
			trace(connection);

		in.close();
		connection.disconnect();
		scanner.close();
	}

	// Private

	/**
	 * Clears the request information, response content and SAP-specific headers.
	 */
	private void reset() {
		this.requestUrl			= null;
		this.requestMethod 		= null;
		this.requestContent 	= null;
		this.responseContent 	= null;
		this.responseMessage 	= null;
		this.responseHeaders 	= null;
		this.responseCode 		= 0;
	}

	/**
	 * Traces the HTTP URL connection.
	 * 
	 * @param connection
	 * @throws Exception
	 */
	private void trace(HttpURLConnection connection) throws Exception {
		String line = "\n" + toString() + "\n";
		line += "=== Headers ===\n";
		for (String key : connection.getHeaderFields().keySet()) {
			line += key + ": " + connection.getHeaderFields().get(key) + "\n";
		}
		// Adds a leading | for console readability
		line = line.replaceAll("\r\n", "\n");
		line = line.replaceAll("\n", "\n| ");
		line = line.replaceAll("\n\\| $", "");
		System.out.println(line);
	}

	/**
	 * Displays the request and the response information.
	 */
//	@SuppressWarnings("boxing")
//	@Override
	public String toString() {
		String message = "[%s] %s\n" + "=== Request content ===\n" 	+ 
				"%s\n" 	 + "=== Response code ===\n" 	+ 
				"%d\n"		 + "=== Response message ===\n" + 
				"%s\n" 	 + "=== Response content ===\n" + "%s";
		return String.format(message, this.requestMethod, this.requestUrl, 
				this.requestContent, this.responseCode, this.responseMessage, 
				this.responseContent);
	}
}

