/**
 * 
 */
package pl.piomin.microservices.edge;

import javax.servlet.RequestDispatcher;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

/**
 * @author mahesh.srinivas
 *
 */
@Component
public class CustomErrorFilter extends ZuulFilter {

    private static final Logger LOG = LoggerFactory.getLogger(CustomErrorFilter.class);
    
    @Value("${error.path:/error}") 
    private String errorPath; 
    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return -1; // Needs to run before SendErrorFilter which has filterOrder == 0
    }

    @Override
    public boolean shouldFilter() {
        // only forward to errorPath if it hasn't been forwarded to already
        return RequestContext.getCurrentContext().containsKey("error.status_code");
    }

    @Override
    public Object run() {
        try {
            RequestContext ctx = RequestContext.getCurrentContext();
            Object e = ctx.get("error.exception");
    
            if (e != null && e instanceof Exception) {
                Exception zuulException = (Exception)e;
                LOG.error("Zuul failure detected: " + ExceptionUtils.getRootCauseMessage(zuulException), zuulException);
                ctx.getRequest().setAttribute("javax.servlet.error.exception", ExceptionUtils.getRootCause(zuulException)); 
                ctx.getRequest().setAttribute("javax.servlet.error.status_code", ctx.get("error.status_code")); 
                RequestDispatcher dispatcher = ctx.getRequest().getRequestDispatcher( 
                	     this.errorPath); 
                	   if (dispatcher != null) { 
                	   
                	    if (!ctx.getResponse().isCommitted()) { 
                	     dispatcher.forward(ctx.getRequest(), ctx.getResponse()); 
                	    } 
                	   } 
                // Remove error code to prevent further error handling in follow up filters
              
            }
        }
        catch (Exception ex) {
            LOG.error("Exception filtering in custom error filter", ex);
            ReflectionUtils.rethrowRuntimeException(ex);
        }
        return null;
    }
    
    public void setErrorPath(String errorPath) { 
    	  this.errorPath = errorPath; 
    	 } 
}
