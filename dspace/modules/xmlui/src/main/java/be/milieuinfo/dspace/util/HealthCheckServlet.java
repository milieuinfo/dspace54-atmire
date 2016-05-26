package be.milieuinfo.dspace.util;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class HealthCheckServlet extends HttpServlet{

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		HttpSession session = req.getSession(false);
		if (null != session){
			session.invalidate();
		}
		
		resp.getOutputStream().println("<html><head><title>DSpace</title></head><body>Status Up</body></html>");
	}

	
}
