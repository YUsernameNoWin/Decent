import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class JettyTestServer extends Thread{
	public void run()
	{

		Handler handler=new AbstractHandler()
		{

			@Override
			public void handle(String target, Request req,
					HttpServletRequest request, HttpServletResponse response)
					throws IOException, ServletException {
		        response.setContentType("text/html");
		        response.setStatus(HttpServletResponse.SC_OK);
		        response.getWriter().println("<!DOCTYPE html PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\r\n" + 
		        		"<HTML>\r\n" + 
		        		"   <HEAD>\r\n" + 
		        		"      <TITLE>\r\n" + 
		        		"         HelloWorld\r\n" + 
		        		"      </TITLE>\r\n" + 
		        		"   </HEAD>\r\n" + 
		        		"<BODY>\r\n" + 
		        		"   <H1>Hello</H1>\r\n" + 
		        		"   <P>HELLOWORLD</P> \r\n" + 
		        		"</BODY>\r\n" + 
		        		"</HTML>");
		        ((Request)request).setHandled(true);
				
			}

		};
		 
		Server server = new Server(1337);
		server.setHandler(handler);
		try {
			server.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
