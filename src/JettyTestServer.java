import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;


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
		        response.getWriter().println("<h1>Hello</h1>");
		        ((Request)request).setHandled(true);
		        System.out.println("HANDLED A REQUEST");
				
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
