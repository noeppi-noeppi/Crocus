import com.sun.tools.xjc.Plugin;
import eu.tuxtown.crocus.jaxbnullity.JaxbNullityPlugin;

module tuxtown.crocus.jaxbnullity {
    requires org.jetbrains.annotations; // non-static
    requires org.glassfish.jaxb.xjc;
    
    provides Plugin with JaxbNullityPlugin; // META-INF/services is also required
}
