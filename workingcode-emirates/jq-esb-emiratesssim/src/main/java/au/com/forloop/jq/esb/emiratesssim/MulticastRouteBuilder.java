package au.com.forloop.jq.esb.emiratesssim;

import org.apache.camel.Endpoint;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import au.com.forloop.jq.esb.common.fuse.koko.AuditRouteInputLogger;
import au.com.forloop.jq.esb.common.fuse.koko.AuditRouteOutputLogger;
import au.com.forloop.jq.esb.common.fuse.koko.ErrorRouteInputLogger;
import au.com.forloop.jq.esb.common.fuse.koko.ErrorRouteOutputLogger;
import au.com.forloop.jq.esb.common.fuse.koko.GenericRouteBuilder;
import au.com.forloop.jq.esb.common.fuse.koko.MainRouteInputLogger;
import au.com.forloop.jq.esb.common.fuse.koko.MainRouteOutputLogger;

public class MulticastRouteBuilder extends GenericRouteBuilder {

    private static final Log log = LogFactory.getLog(MulticastRouteBuilder.class);



    /**
    * Configures the routes based on the environment properties in au.com.forloop.jq.esb.environment.cfg
    */
    public void configure() {
    
        super.configure();
        
        // getContext().getShutdownStrategy().setTimeout(3); // DEVELOPMENT USE
        

        // jq2emirates main route configuration. Endpoint resolution needs to happen via context as shown below
        // entering the {{...}} placeholders directly in the from() and to() won't work.
        
        Endpoint fromschedular = getContext().getEndpoint("{{emiratesssim.jq2emirates.fromschedular.schedular}}");
        Endpoint fromjq = getContext().getEndpoint("{{emiratesssim.jq2emirates.fromjq.scheme}}:{{emiratesssim.jq2emirates.fromjq.path}}?move=received&readLock=changed&readLockMinLength=0&delay={{emiratesssim.jq2emirates.fromjq.pollingperiod}}&noop=true&idempotent=false");
        Endpoint jq2ek = getContext().getEndpoint("{{emiratesssim.jq2emirates.jq2ek.scheme}}:{{emiratesssim.jq2emirates.jq2ek.path}}?username={{emiratesssim.jq2emirates.jq2ek.username}}&password={{emiratesssim.jq2emirates.jq2ek.password}}&binary=true&passiveMode=true&localWorkDirectory=/tmp&bufferSize=1048576&disconnect=true&chmod=666");
        Endpoint jq2ul = getContext().getEndpoint("{{emiratesssim.jq2emirates.jq2ul.scheme}}:{{emiratesssim.jq2emirates.jq2ul.path}}?username={{emiratesssim.jq2emirates.jq2ul.username}}&password={{emiratesssim.jq2emirates.jq2ul.password}}&binary=true&passiveMode=true&localWorkDirectory=/tmp&bufferSize=1048576&disconnect=true&chmod=666");
        Endpoint jq2jl = getContext().getEndpoint("{{emiratesssim.jq2emirates.jq2jl.scheme}}:{{emiratesssim.jq2emirates.jq2jl.path}}?username={{emiratesssim.jq2emirates.jq2jl.username}}&password={{emiratesssim.jq2emirates.jq2jl.password}}&binary=true&passiveMode=true&localWorkDirectory=/tmp&bufferSize=1048576&disconnect=true&chmod=666");
        Endpoint toreceived = getContext().getEndpoint("{{emiratesssim.jq2emirates.toreceived.scheme}}:{{emiratesssim.jq2emirates.toreceived.path}}");

        DeadLetterChannelBuilder jq2emirateserrorHandler = (DeadLetterChannelBuilder) deadLetterChannel("direct:deadletterfromjq").useOriginalMessage().retryAttemptedLogLevel(LoggingLevel.WARN);
        if (getEnvIntProperty("{{emiratesssim.jq2emirates.fromjq.retryattempts}}") != null) {
            jq2emirateserrorHandler = (DeadLetterChannelBuilder)jq2emirateserrorHandler.maximumRedeliveries(getEnvIntProperty("{{emiratesssim.jq2emirates.fromjq.retryattempts}}"));
        }
        if (getEnvIntProperty("{{emiratesssim.jq2emirates.fromjq.retrydelay}}") != null) {
            jq2emirateserrorHandler = (DeadLetterChannelBuilder)jq2emirateserrorHandler.redeliveryDelay(getEnvIntProperty("{{emiratesssim.jq2emirates.fromjq.retrydelay}}"));
        }

        
        
        from(fromschedular)
		.pollEnrich(fromjq.getEndpointUri())
        .routeId("emiratesssim.jq2emirates.main")
        .autoStartup("{{emiratesssim.artefact.autostart}}")
        .errorHandler(jq2emirateserrorHandler)
        .process(new MainRouteInputLogger(fromjq))
        .multicast()
        .parallelProcessing()
        .stopOnException()
       // .to(jq2ek)  
       // .pipeline()
        .to(jq2ek)  
        .pipeline()
        .setHeader("CamelFileName").simple("${in.header.CamelFileName.replaceAll(\"EK\",\"UL\")}")
        .to(jq2ul)  
        .pipeline()
        .setHeader("CamelFileName").simple("${in.header.CamelFileName.replaceAll(\"UL\",\"JL\")}")
        .to(jq2jl) 
        
        
        .onCompletion()
		.onCompleteOnly()
		//.multicast()
		//.pipeline()
		.setHeader("subject",simple("{{emiratesssim.jq2emirates.tomail.subject}}"))
		//.setHeader("CamelFileName").simple("${in.header.CamelFileName.replaceAll(\"{{emiratesssim.jq2emirates.dmz2ulssim.filename.replace}}\",\"{{emiratesssim.jq2emirates.dmz2jqssim.filename}}\")}")
		//.setBody(simple("${file:name} {{emiratesssim.jq2emirates.tomail.body}} ${date:now:yyyy}-${date:now:MM}-${date:now:dd} ${date:now:hh}:${date:now:mm}:${date:now:ss}.${date:now:SS}"))
       .setHeader("CamelFileName").simple("${in.header.CamelFileName.replaceAll(\"-to-EK\",\" \")}")	
       .setBody(simple("${file:name} {{emiratesssim.jq2emirates.tomail.body}} ${date:now:yyyy}-${date:now:MM}-${date:now:dd} ${date:now:hh}:${date:now:mm}:${date:now:ss}.${date:now:SS}"))
		.to("{{emiratesssim.jq2emirates.tomail.destination}}").end()
         
        
        .setHeader("CamelFileName").simple("${in.header.CamelFileName.replaceAll(\"JL\",\"EK\")}")
        .to(toreceived)
        .process(new MainRouteOutputLogger(jq2ek, jq2ul, jq2jl, toreceived));         
            
        // jq2emirates audit route (move received inputs to audit location)
        Endpoint fromjq_processed = getContext().getEndpoint("{{emiratesssim.jq2emirates.fromjq.scheme}}:{{emiratesssim.jq2emirates.fromjq.path}}/received?delete=true&delay={{emiratesssim.jq2emirates.fromjq.pollingperiod}}");
        Endpoint fromjq_audit = getContext().getEndpoint("{{emiratesssim.audit.scheme}}:{{emiratesssim.audit.path}}?fileName=emiratesssim/${date:now:yyyy}/${date:now:MM}/${date:now:dd}/jq2emirates/${file:name}");
        from(fromjq_processed).routeId("emiratesssim.jq2emirates.audit").autoStartup("{{emiratesssim.artefact.autostart}}")
        .process(new AuditRouteInputLogger(fromjq_processed))
        .to(fromjq_audit)
        .process(new AuditRouteOutputLogger(fromjq_audit));
        
        // jq2emirates dead letter route (move failed inputs to failed location)
        Endpoint fromjq_failed = getContext().getEndpoint("{{emiratesssim.jq2emirates.fromjq.scheme}}:{{emiratesssim.jq2emirates.fromjq.path}}/failed");
        from("direct:deadletterfromjq").routeId("emiratesssim.jq2emirates.deadletter")
        .process(new ErrorRouteInputLogger())
        .to(fromjq_failed)
        .process(new ErrorRouteOutputLogger(fromjq_failed));
        
          // jq2emirates failure scheduler route
 		from("{{emiratesssim.jq2emirates.fromschedular.failedschedular}}")
 				.pollEnrich(fromjq_failed.getEndpointUri())
 				.routeId("emiratesssim.jq2emirates.failed")
 				.autoStartup("{{emiratesssim.artefact.autostart}}")
 				.errorHandler(jq2emirateserrorHandler)
 				.setHeader("subject",
 						simple("{{emiratesssim.jq2emirates.tomail.subject}}"))
 				.setBody(
 						simple("${{emiratesssim.jq2emirates.tomail.failurebody}}"))
 				.to("{{emiratesssim.jq2emirates.tomail.destination}}");
         
         
         
        
    }

    
}

