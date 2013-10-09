package test;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class TestFilter implements Filter {
    private static final Logger log = Logger.getLogger(TestFilter.class.getName());
    
    public void init(FilterConfig config) throws ServletException {
    }
    
	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        log.warning("Logging test; invoked inside a servlet filter");
		chain.doFilter(request, response);
	}
}
