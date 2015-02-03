package au.com.forloop.jq.esb.emiratesssim;

import java.util.Properties;

import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;


import au.com.forloop.jq.esb.common.fuse.koko.GenericRouteBuilder;
import au.com.forloop.jq.esb.common.fuse.koko.testing.CamelBlueprintUnitTest;

/**
 * Test the functionality of the route, without endpoint specific behaviour.
 * This covers validation, routing to different endpoints, transformation,
 * enrichment, error handling, ...
 */
public class RouteTest extends CamelBlueprintUnitTest {



    private static final Log log = LogFactory.getLog(RouteTest.class);



    @Override
    protected void modifyRoutes() throws Exception {

        log.info("Modifying routes...");
        final GenericRouteBuilder routeBuilder = new GenericRouteBuilder();
        routeBuilder.loadPropertiesIfNeeded();        
 
        // modify jq2emirates
        RouteDefinition jq2emirates = getRouteDefinition("emiratesssim.jq2emirates.main");
        jq2emirates.autoStartup(true);
        jq2emirates.adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:fromjq");
                mockEndpointsAndSkip(getEndpointValueFromEnvProperty("emiratesssim.jq2emirates.jq2ek"));
                mockEndpointsAndSkip(getEndpointValueFromEnvProperty("emiratesssim.jq2emirates.jq2ul"));
                mockEndpointsAndSkip(getEndpointValueFromEnvProperty("emiratesssim.jq2emirates.jq2jl"));
                mockEndpointsAndSkip(getEndpointValueFromEnvProperty("emiratesssim.jq2emirates.toreceived"));
            }
        });

        log.info("...modified routes");
    }


    
    /**
     * Check that the unittesting environment settings in /src/test/resources/au.com.forloop.jq.esb.environment.cfg are used here
     */
    @Test
    public void test_configuration() throws Exception {
        String environmentConfigurationId = getEnvProperty("emiratesssim.spec.environment.id");
        assertEquals("unittest", environmentConfigurationId);
    }



    @Test
    @Ignore
    public void test_routeLogic_jq2emirates() throws Exception {
        testFromToMocks("jq2emirates", "direct:fromjq", 
           "{{emiratesssim.jq2emirates.jq2ek.scheme}}:{{emiratesssim.jq2emirates.jq2ek.path}}",  
           "{{emiratesssim.jq2emirates.jq2ul.scheme}}:{{emiratesssim.jq2emirates.jq2ul.path}}",  
           "{{emiratesssim.jq2emirates.jq2jl.scheme}}:{{emiratesssim.jq2emirates.jq2jl.path}}",  
           "{{emiratesssim.jq2emirates.toreceived.scheme}}:{{emiratesssim.jq2emirates.toreceived.path}}"  
      );
    }



    @Test
@Ignore // JQESBREF-74
    public void test_errorHandling_jq2emirates() throws Exception {
        addErrorThrowerToRoute("jq2emirates", "fromjq", "jq2emirateserrorHandler", "emiratesssim.jq2emirates.fromjq");
        testDeadletterRoute("direct:fromjq", "emiratesssim.jq2emirates.fromjq");
    }
    
    

    /**
     * This is the place to dynamically override properties. Unit tests NEVER should auto-start their routes
     */
    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties extra = new Properties();
        extra.put("emiratesssim.artefact.autostart", "false");
        return extra;
    }

}

    

