import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;

import java.net.URL;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.extractors.CommonExtractors;
import de.l3s.boilerpipe.sax.HTMLHighlighter;

public class Main extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    if (req.getRequestURI().endsWith("/db")) {
      showDatabase(req,resp);
    } else if (req.getRequestURI().endsWith("/htmlExtract")) {
      doHtmlExtract(req,resp);
    } else if (req.getRequestURI().endsWith("/textExtract")) {
      doTextExtract(req,resp);
    } else {
      showHome(req,resp);
    }
  }

  private void showHome(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.getWriter().print("Hello from Java!");
  }

  private void doTextExtract(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try {
      if(req.getParameter("href") != null) {
        final URL url = new URL(req.getParameter("href"));
        String articleText = ArticleExtractor.INSTANCE.getText(url);
        // System.out.println(articleText);
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().print(articleText);
        resp.getWriter().close();
      } else {
        resp.getWriter().print("No article href sent");
      }
    }
    catch(Exception e) {
      resp.getWriter().println("Error parsing article: "  + req.getParameter("href") + ", error message: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void doHtmlExtract(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try {
      if(req.getParameter("href") != null) {
        final URL url = new URL(req.getParameter("href"));

        final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;
        // final HTMLHighlighter hh = HTMLHighlighter.newHighlightingInstance();
        
        final HTMLHighlighter hh = HTMLHighlighter.newExtractingInstance();
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        out.println("<base href=\"" + url + "\" >");
        out.println("<meta http-equiv=\"Content-Type\" content=\"text-html; charset=utf-8\" />");
        String articleHtml = hh.process(url, extractor);
        System.out.println("test logging");
        // System.out.println(articleHtml);
        out.println(articleHtml);
        out.close();
      } else {
        resp.getWriter().print("No article href sent");
      }
    }
    catch(Exception e) {
      resp.getWriter().println("Error parsing article: "  + req.getParameter("href") + ", error message: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void doHtmlStringExtract(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try {
      // TODO get the html string from the boiling water servlet(or from phantom?, or from a local file for testing?), then pass it to the extractor and return the extracted html
        final URL url = new URL(req.getParameter("href"));

        final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;
        // final HTMLHighlighter hh = HTMLHighlighter.newHighlightingInstance();
        
        final HTMLHighlighter hh = HTMLHighlighter.newExtractingInstance();
        PrintWriter out = resp.getWriter();
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        out.println("<base href=\"" + url + "\" >");
        // out.println("<meta http-equiv=\"Content-Type\" content=\"text-html; charset=utf-8\" />");
        out.println(hh.process(url, extractor));
        out.close();

    }
    catch(Exception e) {
      resp.getWriter().println("Error parsing article: "  + req.getParameter("href") + ", error message: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void showDatabase(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    try {
      Connection connection = getConnection();

      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
      stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
      ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");

      String out = "Hello!\n";
      while (rs.next()) {
          out += "Read from DB: " + rs.getTimestamp("tick") + "\n";
      }

      resp.getWriter().print(out);
    } catch (Exception e) {
      resp.getWriter().print("There was an error: " + e.getMessage());
    }
  }

  private Connection getConnection() throws URISyntaxException, SQLException {
    URI dbUri = new URI(System.getenv("DATABASE_URL"));

    String username = dbUri.getUserInfo().split(":")[0];
    String password = dbUri.getUserInfo().split(":")[1];
    String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + dbUri.getPath();

    return DriverManager.getConnection(dbUrl, username, password);
  }

  public static void main(String[] args) throws Exception{
    Server server = new Server(Integer.valueOf(System.getenv("PORT")));
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);
    context.addServlet(new ServletHolder(new Main()),"/*");
    server.start();
    server.join();
  }
}
