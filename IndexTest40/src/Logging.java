
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class Logging {
  private static Logger Log = LoggerFactory.getLogger(Logging.class);
  
  private static boolean Configured = false;
  
  private static boolean ConfigureUsingFile(JoranConfigurator configurator, String path) {
    boolean configured = false;
    try {
      configurator.doConfigure(path);
      configured = true;
    }
    catch (JoranException e) {
      e.printStackTrace();
    }
    return configured;
  }
  
  private static boolean ConfigureUsingFiles(JoranConfigurator configurator) {
    String configs[] = {"logback.xml", "conf\\logback.xml"};
    for (String config : configs) {
      if (IndexConfig.FileExists(config) && ConfigureUsingFile(configurator, config))
        return true;
    }
    return false;
  }
  
  public synchronized static void Configure() {
    if (Configured)
      return;
    
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(context);
    context.reset();
    ConfigureUsingFiles(configurator);
    StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    Log.info("************************************");
    Configured = true;
  }
}
